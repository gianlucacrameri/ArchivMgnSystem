import { useEffect, useRef, type ReactNode } from "react";
import { IconClose } from "./Icons";

interface Props {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export default function Modal({ title, onClose, children }: Props) {
  // onClose ist bei den Aufrufern meist eine Inline-Funktion, die bei jedem
  // Render der Elternkomponente neu entsteht. Über eine Ref vermeiden wir,
  // dass der Scroll-Lock-Effekt unten dadurch bei jedem Render erneut
  // ab- und wieder angeschaltet wird (sichtbares Scrollbar-Flackern).
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCloseRef.current();
    };
    document.addEventListener("keydown", onKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = "";
    };
  }, []);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-panel"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-panel__header">
          <h2>{title}</h2>
          <button type="button" className="modal-panel__close" onClick={onClose} aria-label="Schliessen">
            <IconClose />
          </button>
        </div>
        <div className="modal-panel__body">{children}</div>
      </div>
    </div>
  );
}
