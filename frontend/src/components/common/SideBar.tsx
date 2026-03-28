import { Box, List, ListItemButton, ListItemText, Divider, Typography } from "@mui/material";
import SpaceDashboardOutlinedIcon from "@mui/icons-material/SpaceDashboardOutlined";
import PostAddOutlinedIcon from "@mui/icons-material/PostAddOutlined";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import HistoryEduOutlinedIcon from "@mui/icons-material/HistoryEduOutlined";
import BusinessIcon from "@mui/icons-material/Business";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import DescriptionIcon from "@mui/icons-material/Description";
import PeopleIcon from "@mui/icons-material/People";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import { darken, useTheme } from "@mui/material/styles";

export function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const theme = useTheme();

  const sidebarBg = darken(theme.palette.primary.main, 0.18);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <Box
      sx={{
        width: 260,
        backgroundColor: sidebarBg,
        color: "#fff",
        display: "flex",
        flexDirection: "column",
        height: "100vh",
      }}
    >
      <Box sx={{ p: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold", mb: 1 }}>
          Portal Partners
        </Typography>
        <Typography variant="caption" sx={{ opacity: 0.8 }}>
          {user?.email}
        </Typography>
        <Typography variant="caption" sx={{ display: "block", opacity: 0.8 }}>
          Perfil: {user?.role}
        </Typography>
      </Box>

      <Divider sx={{ borderColor: "rgba(255,255,255,0.2)" }} />

      <List sx={{ flex: 1, pt: 2 }}>
        <ListItemButton
          onClick={() => navigate("/dashboard")}
          selected={isActive("/dashboard")}
          sx={{
            "&.Mui-selected": {
              backgroundColor: "rgba(255,255,255,0.15)",
            },
            "&:hover": {
              backgroundColor: "rgba(255,255,255,0.1)",
            },
          }}
        >
          <SpaceDashboardOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Dashboard" />
        </ListItemButton>

        <ListItemButton
          onClick={() => navigate("/documentos")}
          selected={isActive("/documentos")}
          sx={{
            "&.Mui-selected": {
              backgroundColor: "rgba(255,255,255,0.15)",
            },
            "&:hover": {
              backgroundColor: "rgba(255,255,255,0.1)",
            },
          }}
        >
          <DescriptionIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Documentos" />
        </ListItemButton>

        {user?.role === "CONTRATADA" && (
          <>
            <ListItemButton
              onClick={() => navigate("/upload-documento")}
              selected={isActive("/upload-documento")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <PostAddOutlinedIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Upload Documento" />
            </ListItemButton>

            <ListItemButton
              onClick={() => navigate("/funcionarios")}
              selected={isActive("/funcionarios")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <PersonAddIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Funcionários" />
            </ListItemButton>
          </>
        )}

        {user?.role === "CONTRATANTE" && (
          <>
            <ListItemButton
              onClick={() => navigate("/contratante/usuarios")}
              selected={isActive("/contratante/usuarios")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <PeopleIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Usuários" />
            </ListItemButton>

            <ListItemButton
              onClick={() => navigate("/contratadas")}
              selected={isActive("/contratadas")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <BusinessIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Contratadas" />
            </ListItemButton>
          </>
        )}

        {user?.role === "ADMIN" && (
          <>
            <ListItemButton
              onClick={() => navigate("/admin/contratantes")}
              selected={isActive("/admin/contratantes")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <BusinessIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Contratantes" />
            </ListItemButton>

            <ListItemButton
              onClick={() => navigate("/admin/contratadas")}
              selected={isActive("/admin/contratadas")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <BusinessIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Contratadas" />
            </ListItemButton>

            <ListItemButton
              onClick={() => navigate("/admin/funcionarios")}
              selected={isActive("/admin/funcionarios")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <PeopleIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Funcionários" />
            </ListItemButton>

            <ListItemButton
              onClick={() => navigate("/admin/audit-log")}
              selected={isActive("/admin/audit-log")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <HistoryEduOutlinedIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Logs de Auditoria" />
            </ListItemButton>

            <ListItemButton
              onClick={() => navigate("/relatorios")}
              selected={isActive("/relatorios")}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "rgba(255,255,255,0.15)",
                },
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                },
              }}
            >
              <AssessmentOutlinedIcon sx={{ marginRight: 2 }} />
              <ListItemText primary="Relatórios" />
            </ListItemButton>
          </>
        )}
      </List>

      <Divider sx={{ borderColor: "rgba(255,255,255,0.2)" }} />

      <List>
        <ListItemButton
          onClick={handleLogout}
          sx={{
            "&:hover": {
              backgroundColor: "rgba(255,255,255,0.1)",
            },
          }}
        >
          <LogoutOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Sair" />
        </ListItemButton>
      </List>
    </Box>
  );
}
