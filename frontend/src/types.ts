export type UserRole = "ADMIN" | "USER" | "MANAGER";

export type TransactionType = "ADD" | "USE";
export type TransactionStatus = "PENDING" | "COMPLETED" | "CANCELED";
export type StockPrecision = "EXACT" | "APPROXIMATE";

export interface UserDTO {
  id: number;
  username: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
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
  createdAt: string;
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
}

export interface ApiResponse {
  status?: number;
  message?: string;
  token?: string;
  role?: UserRole;
  expirationTime?: string;

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
