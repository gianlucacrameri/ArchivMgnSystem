import { apiGetRaw, apiPostRaw } from "./client";

export interface AdminOverview {
  pendingRequests: number;
  pendingRegistrations: number;
}

export interface BackupStatus {
  enabled: boolean;
  backupDirectory?: string;
  photoDirectory: string;
  keepDays: number;
  lastBackupAt?: string;
  lastBackupSizeBytes?: number;
  databaseBackupCount: number;
  photoCount: number;
  lastError?: string;
  lastErrorAt?: string;
}

export interface ActivityLogEntry {
  id: number;
  createdAt: string;
  category: string;
  actorName: string;
  message: string;
}

export async function fetchActivityLog(params: {
  page: number;
  size: number;
  category?: string;
  search?: string;
}): Promise<{ entries: ActivityLogEntry[]; totalElements: number }> {
  const query = new URLSearchParams({ page: String(params.page), size: String(params.size) });
  if (params.category) query.set("category", params.category);
  if (params.search) query.set("search", params.search);
  return apiGetRaw(`/api/admin/log?${query.toString()}`);
}

export async function fetchAdminOverview(): Promise<AdminOverview> {
  return apiGetRaw<AdminOverview>("/api/admin/overview");
}

export async function fetchBackupStatus(): Promise<BackupStatus> {
  return apiGetRaw<BackupStatus>("/api/admin/backup");
}

export async function runBackup(): Promise<BackupStatus> {
  return apiPostRaw<BackupStatus>("/api/admin/backup/run");
}

/**
 * Die Zähler in der Navigation ("Anfragen 3") werden neu geladen, sobald
 * irgendwo ein Antrag entschieden oder ein Konto freigegeben wurde.
 */
const ADMIN_COUNTS_EVENT = "archiv:admin-counts-changed";

export function notifyAdminCountsChanged(): void {
  window.dispatchEvent(new Event(ADMIN_COUNTS_EVENT));
}

export function onAdminCountsChanged(listener: () => void): () => void {
  window.addEventListener(ADMIN_COUNTS_EVENT, listener);
  return () => window.removeEventListener(ADMIN_COUNTS_EVENT, listener);
}
