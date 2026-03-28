import React, { createContext, useContext, useMemo, useState } from "react";
import api from "../services/api";

type Role = "ADMIN" | "CONTRATANTE" | "CONTRATADA";

export type AuthUser = {
  nome?: string | null;
  email: string;
  role: Role;
  perfilId: number | null;
  mustChangePassword: boolean;
};

type LoginRequest = {
  email: string;
  senha: string;
};

type AuthContextData = {
  user: AuthUser | null;
  token: string | null;
  login: (credentials: LoginRequest) => Promise<AuthUser>;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (role: Role) => boolean;
};

const STORAGE_TOKEN_KEY = "@PortalPartners:token";
const STORAGE_USER_KEY = "@PortalPartners:user";

const AuthContext = createContext<AuthContextData | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem(STORAGE_TOKEN_KEY);
  });

  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem(STORAGE_USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  });

  const isAuthenticated = !!token;

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
  };

  const login = async (credentials: LoginRequest) => {
    const response = await api.post("/api/auth/login", credentials);

    const nextToken: string | undefined = response.data?.token;
    const nome: string | undefined = response.data?.nome;
    const email: string | undefined = response.data?.email;
    const role: Role | undefined = response.data?.role;
    const perfilId: number | null = response.data?.perfilId ?? null;
    const mustChangePassword: boolean = !!response.data?.mustChangePassword;

    if (!nextToken || !email || !role) {
      throw new Error("Resposta de autenticação inválida");
    }

    const nextUser: AuthUser = { nome, email, role, perfilId, mustChangePassword };

    setToken(nextToken);
    setUser(nextUser);

    localStorage.setItem(STORAGE_TOKEN_KEY, nextToken);
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(nextUser));

    return nextUser;
  };

  const hasRole = (role: Role) => {
    return user?.role === role;
  };

  const value = useMemo<AuthContextData>(() => {
    return {
      user,
      token,
      login,
      logout,
      isAuthenticated,
      hasRole,
    };
  }, [user, token, isAuthenticated]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextData {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  }
  return ctx;
}
