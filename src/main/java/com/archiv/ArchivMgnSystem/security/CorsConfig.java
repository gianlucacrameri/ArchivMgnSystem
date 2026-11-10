package com.archiv.ArchivMgnSystem.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legt fest, von welchen Webseiten aus der Browser die API aufrufen darf.
 *
 * Mit APP_CORS_ALLOWED_ORIGINS gilt genau diese Liste (z.B.
 * https://archiv.example.ch, mehrere mit Komma getrennt).
 *
 * Ohne diese Angabe darf jede Seite zugreifen, die erkennbar zur eigenen
 * Installation gehört - egal über welche Adresse man den Rechner erreicht:
 *   - localhost,
 *   - Adressen aus privaten Netzen (WLAN zu Hause, Hotspot, VPN),
 *   - dieselbe Adresse, unter der auch das Backend angesprochen wird
 *     (Frontend und Backend auf demselben Rechner bzw. derselben Domain),
 *   - jede Adresse und der Name dieses Rechners selbst.
 * Fremde Webseiten aus dem Internet erfüllen keine dieser Bedingungen.
 */
@Configuration
@Slf4j
public class CorsConfig {

    private static final Pattern ORIGIN = Pattern.compile("^https?://(\\[[0-9a-fA-F:.%]+]|[^/:\\[\\]]+)(?::\\d{1,5})?$");
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final long OWN_ADDRESSES_MAX_AGE_MILLIS = 60_000;
    private static final int MAX_LOGGED_REJECTIONS = 50;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProperty;

    private volatile Set<String> ownAddresses = Set.of();
    private volatile long ownAddressesLoadedAt;
    private final Set<String> loggedRejections = ConcurrentHashMap.newKeySet();

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> configuredOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
                .filter(origin -> !origin.isEmpty())
                .toList();

        return request -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
            configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
            configuration.setMaxAge(1800L);

            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin == null) {
                return configuration;
            }
            boolean allowed = configuredOrigins.isEmpty()
                    ? belongsToThisInstallation(origin, request)
                    : configuredOrigins.contains(origin);
            if (allowed) {
                configuration.setAllowedOrigins(List.of(origin));
            } else if (loggedRejections.size() < MAX_LOGGED_REJECTIONS && loggedRejections.add(origin)) {
                // Ohne diesen Hinweis sieht man nur eine leere Seite und sucht lange.
                log.warn("Aufruf von der Webseite {} abgelehnt. Gehört sie zum Archiv, muss sie in "
                        + "APP_CORS_ALLOWED_ORIGINS eingetragen werden.", origin);
            }
            return configuration;
        };
    }

    private boolean belongsToThisInstallation(String origin, HttpServletRequest request) {
        Matcher matcher = ORIGIN.matcher(origin);
        if (!matcher.matches()) {
            return false;
        }
        String host = normalizeHost(matcher.group(1));

        return isLocalName(host)
                || isPrivateAddress(host)
                || host.equals(normalizeHost(request.getServerName()))
                || ownAddresses().contains(host);
    }

    private static String normalizeHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        int zone = normalized.indexOf('%');
        return zone >= 0 ? normalized.substring(0, zone) : normalized;
    }

    /** Namen, die es nur im eigenen Netz geben kann - nie als öffentliche Webseite. */
    private static boolean isLocalName(String host) {
        if (host.contains(":") || IPV4.matcher(host).matches()) {
            return false;
        }
        return host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || !host.contains(".");
    }

    private static boolean isPrivateAddress(String host) {
        Matcher ipv4 = IPV4.matcher(host);
        if (ipv4.matches()) {
            int first = Integer.parseInt(ipv4.group(1));
            int second = Integer.parseInt(ipv4.group(2));
            return first == 127                                  // dieser Rechner
                    || first == 10                               // private Netze
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 169 && second == 254)           // ohne Router vergeben
                    || (first == 100 && second >= 64 && second <= 127); // Provider-/VPN-Netze
        }
        if (host.contains(":")) {
            return host.equals("::1")
                    || host.startsWith("fc") || host.startsWith("fd")   // private IPv6-Netze
                    || host.startsWith("fe8") || host.startsWith("fe9")
                    || host.startsWith("fea") || host.startsWith("feb");
        }
        return false;
    }

    /** Alle Adressen und der Name dieses Rechners; Adressen können sich ändern (WLAN-Wechsel). */
    private Set<String> ownAddresses() {
        long now = System.currentTimeMillis();
        if (now - ownAddressesLoadedAt < OWN_ADDRESSES_MAX_AGE_MILLIS) {
            return ownAddresses;
        }
        Set<String> addresses = new HashSet<>();
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    addresses.add(normalizeHost(address.getHostAddress()));
                }
            }
            addresses.add(normalizeHost(InetAddress.getLocalHost().getHostName()));
        } catch (Exception e) {
            log.debug("Could not determine own network addresses: {}", e.getMessage());
        }
        ownAddresses = addresses;
        ownAddressesLoadedAt = now;
        return addresses;
    }
}
