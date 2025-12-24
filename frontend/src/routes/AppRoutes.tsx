import { BrowserRouter, Route, Navigate, Routes } from "react-router-dom";
import { Login } from "../pages/Login";
import { ForgotPassword } from "../pages/ForgotPassword";
import React from "react";
import { Dashboard } from "../pages/Dashboard";
import { ReportFiles } from "../pages/ReportFiles";
import { PostFiles } from "../pages/PostFiles";

type UserRole = "contratante" | "terceirizada";

interface ProtectedRouteProps {
  children: React.ReactNode;
  role?: UserRole;
}

// function ProtectedRoute({ children, role }: ProtectedRouteProps): JSX.Element {
//   const isAuthenticated = true;
//   const userRole: UserRole = "contratante";

//   if (!isAuthenticated) {
//     return <Navigate to="/login" replace />;
//   }

//   if (role && role !== userRole) {
//     return <Navigate to="/login" replace />;
//   }

//   return children;
// }

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/report-files" element={<ReportFiles />} />
        <Route path="/post-files" element={<PostFiles />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
