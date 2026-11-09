import { type ChangeEvent } from "react";

interface Props {
  value: number | "";
  onChange: (value: number | "") => void;
  min?: number;
  required?: boolean;
  fallback?: number;
  placeholder?: string;
}

/**
 * Normales <input type="number"> lässt beim Tippen kurzzeitig führende
 * Nullen zu (z.B. "0" -> "05"), wenn der State mit 0 vorbelegt ist und das
 * Feld beim Löschen sofort wieder auf 0 zurückfällt. Deshalb erlauben wir
 * hier einen leeren Zwischenzustand ("") und setzen den Fallback-Wert erst
 * beim Verlassen des Felds, falls es leer geblieben ist.
 */
export default function NumberField({ value, onChange, min, required, fallback, placeholder }: Props) {
  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    const raw = e.target.value;
    onChange(raw === "" ? "" : Number(raw));
  }

  function handleBlur() {
    if (value === "" && fallback !== undefined) {
      onChange(fallback);
    }
  }

  return (
    <input
      type="number"
      inputMode="numeric"
      min={min}
      required={required}
      placeholder={placeholder}
      value={value}
      onChange={handleChange}
      onBlur={handleBlur}
    />
  );
}
