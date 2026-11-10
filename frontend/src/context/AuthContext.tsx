import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { clearAuth, getStoredRole, getToken, setUnauthorizedHandler, storeAuth } from "../api/client";
import { login as loginRequest } from "../api/auth";
import { fetchCurrentUser } from "../api/users";
import type { UserRole } from "../types";

interface AuthState {
  token: string | null;
  role: UserRole | null;
  userId: number | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  /** Ersetzt das Login-Token, z.B. nachdem E-Mail oder Passwort geändert wurden. */
  replaceToken: (token: string) => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getToken());
  const [role, setRole] = useState<UserRole | null>(() => (getStoredRole() as UserRole | null));
  const [userId, setUserId] = useState<number | null>(null);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearAuth();
      setToken(null);
      setRole(null);
      setUserId(null);
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  useEffect(() => {
    if (!token) {
      setUserId(null);
      return;
    }
    let cancelled = false;
    fetchCurrentUser()
      .then((user) => {
        if (cancelled) return;
        setUserId(user.id);
        // Die Rolle kann sich seit der Anmeldung geändert haben (ein Admin hat
        // sie vergeben oder entzogen) - massgebend ist, was der Server sagt.
        setRole(user.role);
        storeAuth(token, user.role);
      })
      .catch(() => {
        if (!cancelled) setUserId(null);
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const value = useMemo<AuthState>(
    () => ({
      token,
      role,
      userId,
      isAuthenticated: !!token,
      isAdmin: role === "ADMIN",
      login: async (email: string, password: string) => {
        const result = await loginRequest(email, password);
        storeAuth(result.token, result.role);
        setToken(result.token);
        setRole(result.role);
      },
      logout: () => {
        clearAuth();
        setToken(null);
        setRole(null);
        setUserId(null);
      },
      replaceToken: (newToken: string) => {
        if (role) storeAuth(newToken, role);
        setToken(newToken);
      },
    }),
    [token, role, userId]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
