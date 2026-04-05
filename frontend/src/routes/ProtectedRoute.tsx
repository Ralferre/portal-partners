import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

type Props = {
  children: React.ReactNode;
};

export function ProtectedRoute({ children }: Props): React.ReactElement {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();
  const firstAccessPath = "/primeiro-acesso/alterar-senha";

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.mustChangePassword && location.pathname !== firstAccessPath) {
    return <Navigate to={firstAccessPath} replace />;
  }

  if (!user?.mustChangePassword && location.pathname === firstAccessPath) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
