import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";

interface Toast {
  id: number;
  message: string;
  kind: "success" | "error";
}

interface ToastState {
  showSuccess: (message: string) => void;
  showError: (message: string) => void;
}

const ToastContext = createContext<ToastState | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const counter = useRef(0);

  const push = useCallback((message: string, kind: Toast["kind"]) => {
    const id = ++counter.current;
    setToasts((prev) => [...prev, { id, message, kind }]);
    window.setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  // Stabile Referenz: sonst bekommt jede Komponente, die useToast() nutzt,
  // bei jedem Toast (irgendwo in der App!) ein "neues" Context-Objekt und
  // rendert unnötig neu - das verursachte u.a. das kurze Scrollbar-Flackern
  // beim Schliessen eines Modals nach einer Erfolgsmeldung.
  const value = useMemo<ToastState>(
    () => ({
      showSuccess: (message: string) => push(message, "success"),
      showError: (message: string) => push(message, "error"),
    }),
    [push]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.kind}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastState {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within ToastProvider");
  return ctx;
}
