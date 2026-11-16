import type { ItemDTO } from "../types";
import AuthenticatedImage from "./AuthenticatedImage";
import { expiryStatus, formatExpiryDate } from "../utils/expiry";

interface Props {
  item: ItemDTO;
  onClick: () => void;
}

export default function ItemCard({ item, onClick }: Props) {
  const lowStock = item.stockQuantity <= 0;
  const expiry = item.expirationDate ? expiryStatus(item.expirationDate) : null;

  return (
    <button type="button" className="item-card" onClick={onClick}>
      <div className="item-card__image">
        <AuthenticatedImage src={item.imageUrl} alt={item.name} size="thumb" className="item-card__img" />
      </div>
      <div className="item-card__body">
        <h3 className="item-card__title">{item.name}</h3>
        <p className="item-card__meta">{item.place}</p>
        <span className={`badge ${lowStock ? "badge--warning" : "badge--neutral"}`}>
          {item.stockQuantity} Stk. {lowStock ? "· nicht verfügbar" : "verfügbar"}
        </span>
        {item.expirationDate && expiry === "expired" && (
          <span className="badge badge--warning">abgelaufen am {formatExpiryDate(item.expirationDate)}</span>
        )}
        {item.expirationDate && expiry === "soon" && (
          <span className="badge badge--neutral">läuft ab am {formatExpiryDate(item.expirationDate)}</span>
        )}
      </div>
    </button>
  );
}
