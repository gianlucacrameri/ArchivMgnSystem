/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_CONTACT_INFO?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
