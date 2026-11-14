export type UserRole = "ADMIN" | "USER" | "MANAGER";

/** NEW = neues Material aufnehmen, DELETE = Material löschen (beides als Antrag). */
export type TransactionType = "ADD" | "USE" | "PHOTO" | "NEW" | "DELETE";
/** Welches Foto ein Foto-Antrag ersetzen soll: das des Materials oder das des Lagerorts. */
export type PhotoTarget = "ITEM" | "LOCATION";
export type TransactionStatus = "PENDING" | "COMPLETED" | "CANCELED";
export type StockPrecision = "EXACT" | "APPROXIMATE";
export type ParticipantStatus = "PENDING" | "APPROVED";

export interface UserDTO {
  id: number;
  username: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
  /** false = Konto wartet noch auf die Freigabe durch einen Admin. */
  approved?: boolean;
  createdAt?: string;
}

export interface CategoryDTO {
  id: number;
  name: string;
}

export interface ItemDTO {
  id: number;
  categoryId: number;
  itemId: number;
  name: string;
  sku: string;
  place: string;
  stockQuantity: number;
  stockPrecision: StockPrecision;
  expirationDate?: string;
  description?: string;
  imageUrl?: string;
  locationImageUrl?: string;
  createdAt?: string;
}

export interface TransactionDTO {
  id: number;
  totalItems: number;
  transactionType: TransactionType;
  transactionStatus: TransactionStatus;
  note?: string;
  description?: string;
  /** Nur bei Foto-Anträgen. */
  proposedImageUrl?: string;
  photoTarget?: PhotoTarget;
  /** Name des Materials als Text - bei NEW (gibt es noch nicht) und DELETE (gibt es danach nicht mehr). */
  subjectName?: string;
  /** Nur bei Anträgen auf ein neues Material. */
  proposedPlace?: string;
  proposedDescription?: string;
  proposedCategoryName?: string;
  proposedStockPrecision?: StockPrecision;
  /** Wer den Antrag freigegeben oder abgelehnt hat. */
  decidedByName?: string;
  updatedAt?: string;
  createdAt: string;
  item?: ItemDTO;
  user?: UserDTO;
}

export interface ReservationDTO {
  id: number;
  itemId: number;
  itemName: string;
  itemImageUrl?: string;
  quantity: number;
  collected: boolean;
  createdAt: string;
}

export interface EventParticipantDTO {
  id: number;
  userId: number;
  username: string;
  status: ParticipantStatus;
}

export interface EventDTO {
  id: number;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  createdById?: number;
  createdByName?: string;
  createdAt?: string;
  reservations?: ReservationDTO[];
  participants?: EventParticipantDTO[];
}

export interface ApiResponse {
  status?: number;
  message?: string;
  token?: string;
  role?: UserRole;
  expirationTime?: string;
  temporaryPassword?: string;

  totalPages?: number;
  totalElements?: number;

  user?: UserDTO;
  users?: UserDTO[];

  category?: CategoryDTO;
  categories?: CategoryDTO[];

  item?: ItemDTO;
  items?: ItemDTO[];

  transaction?: TransactionDTO;
  transactions?: TransactionDTO[];

  event?: EventDTO;
  events?: EventDTO[];
}
