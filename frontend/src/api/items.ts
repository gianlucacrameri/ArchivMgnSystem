import { apiGet, apiSend, apiSendForm } from "./client";
import type { ItemDTO, StockPrecision } from "../types";
import { prepareImageForUpload } from "../utils/resizeImage";

export interface ItemsPage {
  items: ItemDTO[];
  totalPages: number;
  totalElements: number;
}

export async function fetchItems(params: {
  page?: number;
  size?: number;
  filter?: string;
  categoryId?: number | null;
}): Promise<ItemsPage> {
  const search = new URLSearchParams();
  search.set("page", String(params.page ?? 0));
  search.set("size", String(params.size ?? 100));
  if (params.filter) search.set("filter", params.filter);
  if (params.categoryId) search.set("categoryId", String(params.categoryId));

  const res = await apiGet(`/api/items/all?${search.toString()}`);
  return {
    items: res.items ?? [],
    totalPages: res.totalPages ?? 1,
    totalElements: res.totalElements ?? 0,
  };
}

export async function createItem(input: {
  name: string;
  place: string;
  stockQuantity: number;
  stockPrecision: StockPrecision;
  categoryId: number;
  description?: string;
  imageFile?: File | null;
  locationImageFile?: File | null;
}): Promise<void> {
  const form = new FormData();
  form.set("name", input.name);
  form.set("place", input.place);
  form.set("stockQuantity", String(input.stockQuantity));
  form.set("stockPrecision", input.stockPrecision);
  form.set("categoryId", String(input.categoryId));
  if (input.description) form.set("description", input.description);
  if (input.imageFile) form.set("imageFile", await prepareImageForUpload(input.imageFile));
  if (input.locationImageFile) {
    form.set("locationImageFile", await prepareImageForUpload(input.locationImageFile));
  }

  await apiSendForm("POST", "/api/items/add", form);
}

export async function updateItem(input: {
  itemId: number;
  name?: string;
  place?: string;
  stockQuantity?: number;
  stockPrecision?: StockPrecision;
  categoryId?: number;
  description?: string;
  imageFile?: File | null;
  locationImageFile?: File | null;
}): Promise<void> {
  const form = new FormData();
  form.set("itemId", String(input.itemId));
  if (input.name) form.set("name", input.name);
  if (input.place) form.set("place", input.place);
  if (input.stockQuantity !== undefined) form.set("stockQuantity", String(input.stockQuantity));
  if (input.stockPrecision) form.set("stockPrecision", input.stockPrecision);
  if (input.categoryId) form.set("categoryId", String(input.categoryId));
  if (input.description !== undefined) form.set("description", input.description);
  if (input.imageFile) form.set("imageFile", await prepareImageForUpload(input.imageFile));
  if (input.locationImageFile) {
    form.set("locationImageFile", await prepareImageForUpload(input.locationImageFile));
  }

  await apiSendForm("PUT", "/api/items/update", form);
}

export async function deleteItem(id: number): Promise<void> {
  await apiSend("DELETE", `/api/items/delete/${id}`);
}
