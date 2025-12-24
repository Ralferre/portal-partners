import { Box, List, ListItemButton, ListItemText } from "@mui/material";
import SpaceDashboardOutlinedIcon from "@mui/icons-material/SpaceDashboardOutlined";
import PostAddOutlinedIcon from "@mui/icons-material/PostAddOutlined";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import LoginOutlinedIcon from "@mui/icons-material/LoginOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import { Link, useLocation } from "react-router-dom";

export function Sidebar() {
  const location = useLocation();

  return (
    <Box
      sx={{
        width: 260,
        backgroundColor: "#1b6c72ff",
        color: "#fff",
        p: 2,
      }}
    >
      <List>
        <ListItemButton href="/dashboard">
          <SpaceDashboardOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Painel" />
        </ListItemButton>

        <ListItemButton href="/post-files">
          <PostAddOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Postar Documentos" />
        </ListItemButton>

        <ListItemButton href="report-files">
          <AssessmentOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Relatórios" />
        </ListItemButton>

        <ListItemButton href="#">
          <NotificationsOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Notificações" />
        </ListItemButton>

        <ListItemButton href="/login">
          <LoginOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Entrar" />
        </ListItemButton>

        <ListItemButton href="*">
          <LogoutOutlinedIcon sx={{ marginRight: 2 }} />
          <ListItemText primary="Sair" />
        </ListItemButton>
      </List>
    </Box>
  );
}
