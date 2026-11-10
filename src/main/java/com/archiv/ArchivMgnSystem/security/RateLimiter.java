package com.archiv.ArchivMgnSystem.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zählt Ereignisse pro Schlüssel (z.B. Fehlversuche pro E-Mail oder pro
 * IP-Adresse) in einem Zeitfenster. Bewusst nur im Arbeitsspeicher: für eine
 * einzelne Server-Instanz reicht das, und nach einem Neustart sind lediglich
 * die Sperren aufgehoben.
 */
@Component
public class RateLimiter {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private record Window(long startedAtMillis, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Sekunden bis zur Freigabe, wenn das Limit erreicht ist - sonst 0. */
    public long secondsUntilAllowed(String key, int limit, Duration window) {
        Window current = windows.get(key);
        if (current == null) {
            return 0;
        }
        long remainingMillis = current.startedAtMillis() + window.toMillis() - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            windows.remove(key, current);
            return 0;
        }
        return current.count() >= limit ? Math.max(1, remainingMillis / 1000) : 0;
    }

    public void record(String key, Duration window) {
        long now = System.currentTimeMillis();
        if (windows.size() >= MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry -> entry.getValue().startedAtMillis() + window.toMillis() <= now);
            if (windows.size() >= MAX_TRACKED_KEYS) {
                windows.clear();
            }
        }
        windows.merge(key, new Window(now, 1), (existing, fresh) ->
                existing.startedAtMillis() + window.toMillis() <= now
                        ? fresh
                        : new Window(existing.startedAtMillis(), existing.count() + 1));
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
