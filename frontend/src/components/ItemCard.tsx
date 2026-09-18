import type { ItemDTO } from "../types";
import AuthenticatedImage from "./AuthenticatedImage";

interface Props {
  item: ItemDTO;
  onClick: () => void;
}

export default function ItemCard({ item, onClick }: Props) {
  const lowStock = item.stockQuantity <= 0;

  return (
    <button type="button" className="item-card" onClick={onClick}>
      <div className="item-card__image">
        <AuthenticatedImage src={item.imageUrl} alt={item.name} className="item-card__img" />
      </div>
      <div className="item-card__body">
        <h3 className="item-card__title">{item.name}</h3>
        <p className="item-card__meta">{item.place}</p>
        <span className={`badge ${lowStock ? "badge--warning" : "badge--neutral"}`}>
          {item.stockQuantity} Stk. {lowStock ? "· nicht verfügbar" : "verfügbar"}
        </span>
      </div>
    </button>
  );
}
