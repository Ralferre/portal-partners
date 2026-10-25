import { BrowserRouter, Route, Navigate, Routes } from "react-router-dom";
import { Suspense, lazy } from "react";
import { Box, CircularProgress } from "@mui/material";
import { ProtectedRoute } from "./ProtectedRoute";

// Code splitting por rota: cada pagina vira um chunk separado, reduzindo o
// JavaScript baixado no primeiro carregamento (antes era um bundle unico).
const Login = lazy(() => import("../pages/Login").then((m) => ({ default: m.Login })));
const ForgotPassword = lazy(() => import("../pages/ForgotPassword").then((m) => ({ default: m.ForgotPassword })));
const ResetPassword = lazy(() => import("../pages/ResetPassword").then((m) => ({ default: m.ResetPassword })));
const FirstAccessChangePassword = lazy(() => import("../pages/FirstAccessChangePassword").then((m) => ({ default: m.FirstAccessChangePassword })));
const Dashboard = lazy(() => import("../pages/Dashboard").then((m) => ({ default: m.Dashboard })));
const Documentos = lazy(() => import("../pages/Documentos").then((m) => ({ default: m.Documentos })));
const UploadDocumento = lazy(() => import("../pages/UploadDocumento").then((m) => ({ default: m.UploadDocumento })));
const Contratadas = lazy(() => import("../pages/Contratadas").then((m) => ({ default: m.Contratadas })));
const UsuariosContratante = lazy(() => import("../pages/UsuariosContratante").then((m) => ({ default: m.UsuariosContratante })));
const Funcionarios = lazy(() => import("../pages/Funcionarios").then((m) => ({ default: m.Funcionarios })));
const AdminContratantes = lazy(() => import("../pages/admin/AdminContratantes").then((m) => ({ default: m.AdminContratantes })));
const AdminContratadas = lazy(() => import("../pages/admin/AdminContratadas").then((m) => ({ default: m.AdminContratadas })));
const AdminFuncionarios = lazy(() => import("../pages/admin/AdminFuncionarios").then((m) => ({ default: m.AdminFuncionarios })));
const AdminAuditLogs = lazy(() => import("../pages/admin/AdminAuditLogs").then((m) => ({ default: m.AdminAuditLogs })));
const Relatorios = lazy(() => import("../pages/Relatorios").then((m) => ({ default: m.Relatorios })));

function Carregando() {
  return (
    <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh" }}>
      <CircularProgress />
    </Box>
  );
}

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Carregando />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route
            path="/primeiro-acesso/alterar-senha"
            element={<ProtectedRoute><FirstAccessChangePassword /></ProtectedRoute>}
          />
          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/documentos" element={<ProtectedRoute><Documentos /></ProtectedRoute>} />
          <Route path="/upload-documento" element={<ProtectedRoute><UploadDocumento /></ProtectedRoute>} />
          <Route path="/contratadas" element={<ProtectedRoute><Contratadas /></ProtectedRoute>} />
          <Route path="/contratante/usuarios" element={<ProtectedRoute><UsuariosContratante /></ProtectedRoute>} />
          <Route path="/funcionarios" element={<ProtectedRoute><Funcionarios /></ProtectedRoute>} />
          <Route path="/admin/contratantes" element={<ProtectedRoute><AdminContratantes /></ProtectedRoute>} />
          <Route path="/admin/contratadas" element={<ProtectedRoute><AdminContratadas /></ProtectedRoute>} />
          <Route path="/admin/funcionarios" element={<ProtectedRoute><AdminFuncionarios /></ProtectedRoute>} />
          <Route path="/admin/audit-log" element={<ProtectedRoute><AdminAuditLogs /></ProtectedRoute>} />
          <Route path="/relatorios" element={<ProtectedRoute><Relatorios /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
