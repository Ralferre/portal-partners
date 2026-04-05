import { BrowserRouter, Route, Navigate, Routes } from "react-router-dom";
import { Login } from "../pages/Login";
import { ForgotPassword } from "../pages/ForgotPassword";
import { ResetPassword } from "../pages/ResetPassword";
import { FirstAccessChangePassword } from "../pages/FirstAccessChangePassword";
import { Dashboard } from "../pages/Dashboard";
import { Documentos } from "../pages/Documentos";
import { UploadDocumento } from "../pages/UploadDocumento";
import { Contratadas } from "../pages/Contratadas";
import { UsuariosContratante } from "../pages/UsuariosContratante";
import { Funcionarios } from "../pages/Funcionarios";
import { AdminContratantes } from "../pages/admin/AdminContratantes";
import { AdminContratadas } from "../pages/admin/AdminContratadas";
import { AdminFuncionarios } from "../pages/admin/AdminFuncionarios";
import { AdminAuditLogs } from "../pages/admin/AdminAuditLogs";
import { Relatorios } from "../pages/Relatorios";
import { ProtectedRoute } from "./ProtectedRoute";

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route
          path="/primeiro-acesso/alterar-senha"
          element={
            <ProtectedRoute>
              <FirstAccessChangePassword />
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/documentos"
          element={
            <ProtectedRoute>
              <Documentos />
            </ProtectedRoute>
          }
        />
        <Route
          path="/upload-documento"
          element={
            <ProtectedRoute>
              <UploadDocumento />
            </ProtectedRoute>
          }
        />
        <Route
          path="/contratadas"
          element={
            <ProtectedRoute>
              <Contratadas />
            </ProtectedRoute>
          }
        />
        <Route
          path="/contratante/usuarios"
          element={
            <ProtectedRoute>
              <UsuariosContratante />
            </ProtectedRoute>
          }
        />
        <Route
          path="/funcionarios"
          element={
            <ProtectedRoute>
              <Funcionarios />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/contratantes"
          element={
            <ProtectedRoute>
              <AdminContratantes />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/contratadas"
          element={
            <ProtectedRoute>
              <AdminContratadas />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/funcionarios"
          element={
            <ProtectedRoute>
              <AdminFuncionarios />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/audit-log"
          element={
            <ProtectedRoute>
              <AdminAuditLogs />
            </ProtectedRoute>
          }
        />

        <Route
          path="/relatorios"
          element={
            <ProtectedRoute>
              <Relatorios />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
