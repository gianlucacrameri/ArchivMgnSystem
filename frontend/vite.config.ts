import { defineConfig, loadEnv, type Plugin } from "vite";
import react from "@vitejs/plugin-react";

/**
 * Schreibt in die gebaute Seite eine Content-Security-Policy: Der Browser
 * führt dann nur Skripte aus, die zur Seite selbst gehören, und erlaubt
 * Datenverkehr nur zum eigenen Backend. Gelangt je fremder Code in die Seite,
 * kann er weder nachgeladen werden noch Daten an andere Server schicken.
 *
 * Nur im Build: der Entwicklungsserver braucht Inline-Skripte und WebSockets.
 */
function contentSecurityPolicy(apiBaseUrl: string): Plugin {
  let apiOrigin = "";
  try {
    apiOrigin = new URL(apiBaseUrl).origin;
  } catch {
    // relative oder leere Adresse: Backend liegt auf derselben Adresse wie die Seite
  }
  const policy = [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self'",
    "font-src 'self' data:",
    // Fotos werden mit Login geladen und als blob:-Adresse angezeigt
    "img-src 'self' blob: data:",
    `connect-src 'self' ${apiOrigin}`.trim(),
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join("; ");

  return {
    name: "content-security-policy",
    apply: "build",
    transformIndexHtml() {
      return [
        {
          tag: "meta",
          attrs: { "http-equiv": "Content-Security-Policy", content: policy },
          injectTo: "head-prepend",
        },
      ];
    },
  };
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "VITE_");
  const apiBaseUrl = env.VITE_API_BASE_URL ?? "http://localhost:8080";

  return {
    plugins: [react(), contentSecurityPolicy(apiBaseUrl)],
    server: {
      host: true,
      port: 5173,
    },
  };
});
