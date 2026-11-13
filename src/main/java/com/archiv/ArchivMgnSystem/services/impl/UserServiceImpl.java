package com.archiv.ArchivMgnSystem.services.impl;

import com.archiv.ArchivMgnSystem.dtos.LoginRequest;
import com.archiv.ArchivMgnSystem.dtos.RegisterRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.UserDTO;
import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.archiv.ArchivMgnSystem.exceptions.ApiException;
import com.archiv.ArchivMgnSystem.exceptions.InvalidCredentialException;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.User;
import com.archiv.ArchivMgnSystem.repositories.EventParticipantRepository;
import com.archiv.ArchivMgnSystem.repositories.EventRepository;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.repositories.UserRepository;
import com.archiv.ArchivMgnSystem.security.AuthUser;
import com.archiv.ArchivMgnSystem.security.JwtUtil;
import com.archiv.ArchivMgnSystem.security.PasswordPolicy;
import com.archiv.ArchivMgnSystem.security.RateLimiter;
import com.archiv.ArchivMgnSystem.services.ActivityLogService;
import com.archiv.ArchivMgnSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    // Nach 5 Fehlversuchen für dieselbe E-Mail ist das Login 15 Minuten gesperrt.
    private static final int MAX_FAILED_LOGINS_PER_EMAIL = 5;
    // Bremst das Durchprobieren vieler Konten von einer Adresse aus.
    private static final int MAX_FAILED_LOGINS_PER_IP = 50;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_REGISTRATIONS_PER_IP = 60;
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // Ohne leicht verwechselbare Zeichen (0/O, 1/l/I), damit man es abtippen kann.
    private static final String TEMPORARY_PASSWORD_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TEMPORARY_PASSWORD_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtUtil jwtUtils;
    private final RateLimiter rateLimiter;
    private final ActivityLogService activityLog;

    /** Für unbekannte E-Mails wird trotzdem ein Hash geprüft, damit die Antwortzeit nichts verrät. */
    private volatile String dummyPasswordHash;

    @Override
    public Response registerUser(RegisterRequest registerRequest, String clientIp) {
        String ipKey = "register:ip:" + clientIp;
        if (rateLimiter.secondsUntilAllowed(ipKey, MAX_REGISTRATIONS_PER_IP, REGISTRATION_WINDOW) > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Zu viele Registrierungen. Bitte später erneut versuchen.");
        }
        PasswordPolicy.validate(registerRequest.getPassword());

        // Selbstregistrierung erhält immer die Rolle USER und ist zunächst gesperrt:
        // ein Admin muss das Konto freigeben, bevor es sich anmelden kann. Höhere
        // Rollen vergibt ausschliesslich ein bestehender Admin (siehe updateUser).
        User userToSave = User.builder()
                .username(registerRequest.getName().trim())
                .email(registerRequest.getEmail().trim())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phoneNumber(registerRequest.getPhoneNumber().trim())
                .role(UserRole.USER)
                .approved(false)
                .build();

        try {
            userRepository.save(userToSave);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Name, E-Mail oder Telefonnummer wird bereits verwendet");
        }
        rateLimiter.record(ipKey, REGISTRATION_WINDOW);
        activityLog.record(ActivityLogService.MEMBER, userToSave.getUsername(),
                "Neues Konto registriert (" + userToSave.getEmail() + "), wartet auf Freigabe");

        return Response.builder()
                .status(200)
                .message("Konto erstellt. Ein Admin muss es freigeben, bevor du dich anmelden kannst.")
                .build();
    }

    @Override
    public Response loginUser(LoginRequest loginRequest, String clientIp) {
        String emailKey = "login:email:" + loginRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        String ipKey = "login:ip:" + clientIp;

        long waitSeconds = Math.max(
                rateLimiter.secondsUntilAllowed(emailKey, MAX_FAILED_LOGINS_PER_EMAIL, LOGIN_WINDOW),
                rateLimiter.secondsUntilAllowed(ipKey, MAX_FAILED_LOGINS_PER_IP, LOGIN_WINDOW));
        if (waitSeconds > 0) {
            long minutes = (waitSeconds + 59) / 60;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Fehlversuche. Bitte in " + minutes + (minutes == 1 ? " Minute" : " Minuten") + " erneut versuchen.");
        }

        Optional<User> found = userRepository.findByEmail(loginRequest.getEmail().trim());
        String password = loginRequest.getPassword();
        boolean passwordMatches = !PasswordPolicy.isTooLong(password)
                && passwordEncoder.matches(password, found.map(User::getPassword).orElseGet(this::dummyPasswordHash));

        if (found.isEmpty() || !passwordMatches) {
            rateLimiter.record(emailKey, LOGIN_WINDOW);
            rateLimiter.record(ipKey, LOGIN_WINDOW);
            // Bewusst dieselbe Meldung für "E-Mail unbekannt" und "Passwort falsch":
            // sonst lässt sich herausfinden, welche Adressen registriert sind.
            throw new InvalidCredentialException("E-Mail oder Passwort ist falsch");
        }

        User user = found.get();
        rateLimiter.reset(emailKey);

        if (!user.isApproved() && user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Dein Konto wurde noch nicht freigegeben. Ein Admin muss es zuerst bestätigen.");
        }

        return Response.builder()
                .status(200)
                .message("User logged in successfully")
                .role(user.getRole())
                .token(jwtUtils.generateToken(user))
                .expirationTime(jwtUtils.getExpirationDays() + " days")
                .build();
    }

    private String dummyPasswordHash() {
        String hash = dummyPasswordHash;
        if (hash == null) {
            hash = passwordEncoder.encode("kein-echtes-passwort");
            dummyPasswordHash = hash;
        }
        return hash;
    }

    @Override
    public Response getAllUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        users.forEach(user -> user.setTransactions(null));

        List<UserDTO> userDTOS = modelMapper.map(users, new TypeToken<List<UserDTO>>() {}.getType());

        return Response.builder()
                .status(200)
                .message("success")
                .users(userDTOS)
                .build();
    }

    @Override
    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User Not Found"));
        user.setTransactions(null);
        return user;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((AuthUser) authentication.getPrincipal()).getUser().getId();
    }

    @Override
    public Response getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User Not Found"));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);


        userDTO.setTransactions(null);

        return Response.builder()
                .status(200)
                .message("success")
                .user(userDTO)
                .build();
    }


    @Override
    public Response updateUser(Long id, UserDTO userDTO) {

        User existingUser = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User Not Found"));

        User currentUser = getCurrentLoggedInUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isSelf = currentUser.getId().equals(id);
        String roleChange = null;

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You are not allowed to update this user");
        }

        if (userDTO.getRole() != null && userDTO.getRole() != existingUser.getRole()) {
            if (!isAdmin) {
                throw new AccessDeniedException("Only admins can change a user's role");
            }
            if (isSelf) {
                // Sonst könnte sich der letzte Admin versehentlich selbst aussperren.
                throw new IllegalArgumentException("Du kannst deine eigene Rolle nicht ändern");
            }
            existingUser.setRole(userDTO.getRole());
            roleChange = userDTO.getRole() == UserRole.ADMIN
                    ? existingUser.getUsername() + " zum Admin gemacht"
                    : "Admin-Rechte von " + existingUser.getUsername() + " entzogen";
        }

        boolean changesEmail = userDTO.getEmail() != null
                && !userDTO.getEmail().trim().equalsIgnoreCase(existingUser.getEmail());
        boolean changesPassword = userDTO.getPassword() != null && !userDTO.getPassword().isEmpty();
        if (isSelf && (changesEmail || changesPassword)) {
            // Mit E-Mail und Passwort lässt sich ein Konto übernehmen. Ein
            // entsperrtes, unbeaufsichtigtes Gerät darf dafür nicht reichen.
            requireCurrentPassword(existingUser, userDTO.getCurrentPassword());
        }

        if (userDTO.getEmail() != null) {
            String email = userDTO.getEmail().trim();
            if (!SIMPLE_EMAIL.matcher(email).matches()) {
                throw new IllegalArgumentException("Bitte eine gültige E-Mail-Adresse angeben");
            }
            existingUser.setEmail(email);
        }
        if (userDTO.getPhoneNumber() != null && !userDTO.getPhoneNumber().isBlank()) {
            existingUser.setPhoneNumber(userDTO.getPhoneNumber().trim());
        }
        if (userDTO.getUsername() != null && !userDTO.getUsername().isBlank()) {
            existingUser.setUsername(userDTO.getUsername().trim());
        }

        if (changesPassword) {
            PasswordPolicy.validate(userDTO.getPassword());
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            if (isSelf) {
                existingUser.setPasswordResetRequired(false);
            }
            // Neues Passwort meldet alle bisherigen Sitzungen dieses Kontos ab.
            existingUser.setTokenVersion(existingUser.getTokenVersion() + 1);
        }

        try {
            userRepository.save(existingUser);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Name, E-Mail oder Telefonnummer wird bereits verwendet");
        }

        if (roleChange != null) {
            activityLog.record(ActivityLogService.MEMBER, roleChange);
        }

        // Das Login-Token hängt an E-Mail und Token-Version. Wer sein eigenes
        // Profil ändert, bekommt deshalb ein neues Token und bleibt angemeldet.
        return Response.builder()
                .status(200)
                .message("User successfully updated")
                .token(isSelf ? jwtUtils.generateToken(existingUser) : null)
                .role(isSelf ? existingUser.getRole() : null)
                .build();
    }

    @Override
    @Transactional
    public Response deleteUser(Long id) {
        if (currentUserId().equals(id)) {
            throw new IllegalArgumentException("Du kannst dein eigenes Konto nicht löschen");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User Not Found"));
        activityLog.record(ActivityLogService.MEMBER, user.isApproved()
                ? "Konto von " + user.getUsername() + " (" + user.getEmail() + ") entfernt"
                : "Registrierung von " + user.getUsername() + " (" + user.getEmail() + ") abgelehnt");

        // Was das Mitglied hinterlässt, bleibt für den Verein erhalten: Events
        // und die Bestandshistorie verlieren nur den Bezug zur Person.
        eventParticipantRepository.deleteByUserId(id);
        eventRepository.clearCreator(id);
        transactionRepository.clearUser(id);
        userRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("User successfully deleted")
                .build();

    }

    @Override
    public Response approveUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User Not Found"));
        user.setApproved(true);
        userRepository.save(user);
        activityLog.record(ActivityLogService.MEMBER, "Konto von " + user.getUsername() + " freigegeben");

        return Response.builder()
                .status(200)
                .message("User successfully approved")
                .build();
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        String key = "password-check:" + user.getId();
        if (rateLimiter.secondsUntilAllowed(key, MAX_FAILED_LOGINS_PER_EMAIL, LOGIN_WINDOW) > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Zu viele Fehlversuche. Bitte später erneut versuchen.");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("Bitte das aktuelle Passwort eingeben");
        }
        if (PasswordPolicy.isTooLong(currentPassword) || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            rateLimiter.record(key, LOGIN_WINDOW);
            throw new ApiException(HttpStatus.FORBIDDEN, "Das aktuelle Passwort ist falsch");
        }
        rateLimiter.reset(key);
    }

    @Override
    public Response resetPassword(Long id) {
        if (currentUserId().equals(id)) {
            throw new IllegalArgumentException("Dein eigenes Passwort änderst du im Profil");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User Not Found"));

        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setPasswordResetRequired(true);
        // Alte Anmeldungen enden, eine allfällige Login-Sperre wird aufgehoben.
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        rateLimiter.reset("login:email:" + user.getEmail().trim().toLowerCase(Locale.ROOT));
        rateLimiter.reset("password-check:" + user.getId());
        activityLog.record(ActivityLogService.MEMBER, "Passwort von " + user.getUsername() + " zurückgesetzt");

        // Das Passwort wird nur dieses eine Mal im Klartext zurückgegeben; der
        // Admin gibt es dem Mitglied persönlich weiter.
        return Response.builder()
                .status(200)
                .message("Temporary password created")
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private String generateTemporaryPassword() {
        while (true) {
            StringBuilder password = new StringBuilder();
            for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) {
                password.append(TEMPORARY_PASSWORD_ALPHABET.charAt(SECURE_RANDOM.nextInt(TEMPORARY_PASSWORD_ALPHABET.length())));
            }
            String candidate = password.toString();
            if (candidate.chars().anyMatch(Character::isLetter) && candidate.chars().anyMatch(Character::isDigit)) {
                return candidate;
            }
        }
    }

    @Override
    public Response logoutEverywhere() {
        User user = userRepository.findById(currentUserId()).orElseThrow(() -> new NotFoundException("User Not Found"));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        return Response.builder()
                .status(200)
                .message("Logged out on all devices")
                .build();
    }

    @Override
    public Response getUserTransactions(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User Not Found"));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        userDTO.getTransactions().forEach(transactionDTO -> {
            transactionDTO.setUser(null);
                });
        return Response.builder()
                .status(200)
                .message("success")
                .user(userDTO)
                .build();
    }
}
