package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.BackupStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Datensicherung: jede Nacht ein vollständiger Abzug der Datenbank
 * (mysqldump, gzip-komprimiert) und eine Kopie aller neuen Fotos.
 *
 * Aktiv, sobald APP_BACKUP_DIR gesetzt ist. Der Ordner sollte auf einem
 * anderen Datenträger liegen als Datenbank und Fotos - sonst hilft die
 * Sicherung bei einem Festplattendefekt nicht.
 *
 * Aufbau:  <Backup-Ordner>/datenbank/archiv-2026-01-31_0300.sql.gz
 *          <Backup-Ordner>/fotos/<Dateiname des Fotos>
 *
 * Fotos werden nur ergänzt, nie gelöscht: so passt auch eine ältere
 * Datenbank-Sicherung noch zu den gesicherten Fotos.
 */
@Service
@Slf4j
public class BackupService {

    private static final Pattern JDBC_URL = Pattern.compile("^jdbc:(?:mysql|mariadb)://([^:/?]+)(?::(\\d+))?/([^?]+)");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final String DATABASE_FOLDER = "datenbank";
    private static final String PHOTO_FOLDER = "fotos";
    private static final String DUMP_PREFIX = "archiv-";
    private static final String DUMP_SUFFIX = ".sql.gz";
    private static final int ALWAYS_KEEP_NEWEST = 3;
    private static final long DUMP_TIMEOUT_MINUTES = 30;

    @Value("${app.backup.dir:}")
    private String backupDir;

    @Value("${app.backup.keep-days:30}")
    private int keepDays;

    @Value("${app.backup.mysqldump-path:mysqldump}")
    private String mysqldumpPath;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${app.upload-dir:${user.dir}/uploads/items}")
    private String uploadDir;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String lastError;
    private volatile LocalDateTime lastErrorAt;

    public boolean isEnabled() {
        return backupDir != null && !backupDir.isBlank();
    }

    @Scheduled(cron = "${app.backup.cron:0 0 3 * * *}")
    public void scheduledBackup() {
        if (isEnabled()) {
            run();
        }
    }

    /** Führt eine Sicherung aus. Fehler landen im Status statt als Ausnahme beim Aufrufer. */
    public BackupStatusDTO run() {
        if (!isEnabled()) {
            throw new IllegalStateException("Es ist kein Backup-Ordner eingerichtet (APP_BACKUP_DIR)");
        }
        if (!lock.tryLock()) {
            throw new IllegalStateException("Es läuft bereits eine Sicherung");
        }
        try {
            Path root = Paths.get(backupDir).toAbsolutePath().normalize();
            Path dump = dumpDatabase(Files.createDirectories(root.resolve(DATABASE_FOLDER)));
            int copiedPhotos = copyNewPhotos(Files.createDirectories(root.resolve(PHOTO_FOLDER)));
            removeOldDumps(root.resolve(DATABASE_FOLDER));
            lastError = null;
            lastErrorAt = null;
            log.info("Backup finished: {} ({} bytes), {} new photos", dump.getFileName(), Files.size(dump), copiedPhotos);
        } catch (Exception e) {
            lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            lastErrorAt = LocalDateTime.now();
            log.error("Backup failed: {}", lastError);
        } finally {
            lock.unlock();
        }
        return status();
    }

    public BackupStatusDTO status() {
        BackupStatusDTO.BackupStatusDTOBuilder status = BackupStatusDTO.builder()
                .enabled(isEnabled())
                .photoDirectory(Paths.get(uploadDir).toAbsolutePath().normalize().toString())
                .keepDays(keepDays)
                .lastError(lastError)
                .lastErrorAt(lastErrorAt);
        if (!isEnabled()) {
            return status.build();
        }

        Path root = Paths.get(backupDir).toAbsolutePath().normalize();
        status.backupDirectory(root.toString());
        try {
            List<Path> dumps = listDumps(root.resolve(DATABASE_FOLDER));
            status.databaseBackupCount(dumps.size());
            if (!dumps.isEmpty()) {
                Path newest = dumps.get(dumps.size() - 1);
                status.lastBackupAt(LocalDateTime.ofInstant(Files.getLastModifiedTime(newest).toInstant(), ZoneId.systemDefault()));
                status.lastBackupSizeBytes(Files.size(newest));
            }
            status.photoCount(listFiles(root.resolve(PHOTO_FOLDER)).size());
        } catch (IOException e) {
            status.lastError("Backup-Ordner ist nicht lesbar: " + e.getMessage());
        }
        return status.build();
    }

    private Path dumpDatabase(Path targetDir) throws IOException, InterruptedException {
        Matcher url = JDBC_URL.matcher(datasourceUrl);
        if (!url.find()) {
            throw new IOException("Datenbank-Adresse wird nicht verstanden: nur jdbc:mysql://host:port/name");
        }
        String host = url.group(1);
        String port = url.group(2) != null ? url.group(2) : "3306";
        String database = url.group(3);

        Path target = targetDir.resolve(DUMP_PREFIX + LocalDateTime.now().format(FILE_TIMESTAMP) + DUMP_SUFFIX);
        Path partial = targetDir.resolve(target.getFileName() + ".partial");
        Path errors = Files.createTempFile("archiv-backup-", ".log");
        // Das Passwort steht in einer nur für diesen Benutzer lesbaren Datei
        // statt in der Befehlszeile (die jeder Benutzer des Servers sehen kann).
        Path credentials = createPrivateTempFile();
        try {
            Files.writeString(credentials, "[client]\npassword=\""
                    + datasourcePassword.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n", StandardCharsets.UTF_8);

            Process process = new ProcessBuilder(
                    mysqldumpPath,
                    "--defaults-extra-file=" + credentials,
                    // Wie das Backend selbst immer über das Netzwerk verbinden - bei
                    // "localhost" würde mysqldump sonst eine Socket-Datei suchen.
                    "--protocol=TCP",
                    "--host=" + host,
                    "--port=" + port,
                    "--user=" + datasourceUsername,
                    "--single-transaction",
                    "--no-tablespaces",
                    "--default-character-set=utf8mb4",
                    database)
                    .redirectError(errors.toFile())
                    .start();

            byte[] tail = new byte[0];
            try (InputStream in = process.getInputStream();
                 OutputStream out = new GZIPOutputStream(Files.newOutputStream(partial))) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    tail = lastBytes(tail, buffer, read);
                }
            }
            if (!process.waitFor(DUMP_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IOException("mysqldump hat nicht innerhalb von " + DUMP_TIMEOUT_MINUTES + " Minuten geantwortet");
            }
            if (process.exitValue() != 0) {
                String message = Files.readString(errors).strip();
                throw new IOException("mysqldump ist fehlgeschlagen: " + (message.length() > 300 ? message.substring(0, 300) : message));
            }
            // mysqldump schreibt diese Zeile ganz am Schluss - fehlt sie, ist der Abzug unvollständig.
            if (!new String(tail, StandardCharsets.UTF_8).contains("Dump completed")) {
                throw new IOException("Der Datenbank-Abzug ist unvollständig");
            }
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Cannot run program")) {
                throw new IOException("Das Programm mysqldump wurde nicht gefunden. Es gehört zum MySQL-Client; "
                        + "der Pfad lässt sich mit APP_BACKUP_MYSQLDUMP_PATH angeben.");
            }
            throw e;
        } finally {
            Files.deleteIfExists(credentials);
            Files.deleteIfExists(errors);
            Files.deleteIfExists(partial);
        }
    }

    private Path createPrivateTempFile() throws IOException {
        try {
            return Files.createTempFile("archiv-backup-", ".cnf",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException e) {
            // Windows kennt diese Rechte nicht; der Temp-Ordner gehört dort ohnehin dem Benutzer.
            return Files.createTempFile("archiv-backup-", ".cnf");
        }
    }

    private static byte[] lastBytes(byte[] previous, byte[] buffer, int read) {
        int keep = 256;
        byte[] combined = new byte[Math.min(keep, previous.length + read)];
        int fromBuffer = Math.min(read, combined.length);
        int fromPrevious = combined.length - fromBuffer;
        System.arraycopy(previous, previous.length - fromPrevious, combined, 0, fromPrevious);
        System.arraycopy(buffer, read - fromBuffer, combined, fromPrevious, fromBuffer);
        return combined;
    }

    /** Kopiert Originalfotos, die im Backup noch fehlen. Vorschaubilder lassen sich neu erzeugen. */
    private int copyNewPhotos(Path targetDir) throws IOException {
        Path source = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            return 0;
        }
        int copied = 0;
        for (Path photo : listFiles(source)) {
            Path target = targetDir.resolve(photo.getFileName().toString());
            if (Files.exists(target) && Files.size(target) == Files.size(photo)) {
                continue;
            }
            Path partial = targetDir.resolve(photo.getFileName() + ".partial");
            Files.copy(photo, partial, StandardCopyOption.REPLACE_EXISTING);
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        return copied;
    }

    private void removeOldDumps(Path databaseDir) throws IOException {
        List<Path> dumps = listDumps(databaseDir);
        Instant limit = Instant.now().minus(keepDays, ChronoUnit.DAYS);
        for (int i = 0; i < dumps.size() - ALWAYS_KEEP_NEWEST; i++) {
            if (Files.getLastModifiedTime(dumps.get(i)).toInstant().isBefore(limit)) {
                Files.deleteIfExists(dumps.get(i));
            }
        }
    }

    /** Datenbank-Sicherungen, älteste zuerst (der Zeitstempel steckt im Dateinamen). */
    private List<Path> listDumps(Path databaseDir) throws IOException {
        return listFiles(databaseDir).stream()
                .filter(file -> {
                    String name = file.getFileName().toString();
                    return name.startsWith(DUMP_PREFIX) && name.endsWith(DUMP_SUFFIX);
                })
                .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                .toList();
    }

    private List<Path> listFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && !entry.getFileName().toString().endsWith(".partial")) {
                    files.add(entry);
                }
            }
        }
        return files;
    }
}
