import {
  AppBar,
  Badge,
  Box,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Menu,
  MenuItem,
  Popover,
  Typography,
  Toolbar,
} from "@mui/material";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import { SearchInput } from "./SearchInput";
import { useEffect, useState } from "react";
import api from "../../services/api";
import { useAuth } from "../../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

const STORAGE_THEME_KEY = "@PortalPartners:theme";

type ThemeKey = "agro" | "forest" | "sunset";

export function Header() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [novosCount, setNovosCount] = useState<number>(0);

  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null);
  const [themeMenuAnchor, setThemeMenuAnchor] = useState<null | HTMLElement>(null);

  const [themeKey, setThemeKey] = useState<ThemeKey>(() => {
    const raw = localStorage.getItem(STORAGE_THEME_KEY);
    if (raw === "forest" || raw === "sunset" || raw === "agro") return raw;
    return "agro";
  });

  const [searchQuery, setSearchQuery] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchResults, setSearchResults] = useState<SearchResponse | null>(null);
  const [searchAnchor, setSearchAnchor] = useState<HTMLDivElement | null>(null);

  useEffect(() => {
    const load = async () => {
      if (user?.role !== "CONTRATANTE") {
        setNovosCount(0);
        return;
      }
      try {
        const resp = await api.get<number>("/api/documentos/novos/count");
        setNovosCount(resp.data ?? 0);
      } catch {
        // silencioso para não poluir header
      }
    };

    load();
  }, [user?.role]);

  const closeSearch = () => {
    setSearchOpen(false);
  };

  const handleNavigate = (path: string) => {
    closeSearch();
    setSearchQuery("");
    navigate(path);
  };

  useEffect(() => {
    const q = searchQuery.trim();
    if (!searchOpen) return;
    if (q.length < 2) {
      setSearchResults(null);
      return;
    }

    setSearchLoading(true);
    const t = window.setTimeout(async () => {
      try {
        const resp = await api.get<SearchResponse>(`/api/search?q=${encodeURIComponent(q)}`);
        setSearchResults(resp.data);
      } catch {
        setSearchResults(null);
      } finally {
        setSearchLoading(false);
      }
    }, 300);

    return () => {
      window.clearTimeout(t);
    };
  }, [searchQuery, searchOpen]);

  const handleLogout = () => {
    setUserMenuAnchor(null);
    logout();
  };

  const handleSelectTheme = (next: ThemeKey) => {
    setThemeKey(next);
    localStorage.setItem(STORAGE_THEME_KEY, next);
    setThemeMenuAnchor(null);
    window.dispatchEvent(new Event("portalpartners:theme"));
  };

  return (
    // <Box height={60} sx={{ flexGrow: 1 }}>
    <AppBar position="static" elevation={0}>
      <Toolbar>
        <IconButton>
          <HomeOutlinedIcon sx={{ color: "white" }} />
          <Typography
            variant="h6"
            paddingLeft={1}
            fontSize={16}
            color="white"
            noWrap
            component="div"
            sx={{ display: { xs: "none", sm: "block" } }}
          >
            Portal Partners
          </Typography>
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Box
            sx={{ display: { xs: "none", md: "flex" } }}
            justifyContent="flex-end"
          >
            <Box
              sx={{
                flexGrow: 1,
                display: "flex",
                justifyContent: "right",
                ml: 4,
                mr: 3,
              }}
            >
              <Box sx={{ display: { xs: "none", md: "block" } }}>
                <SearchInput
                  ref={(el) => setSearchAnchor(el)}
                  value={searchQuery}
                  onChange={(v) => setSearchQuery(v)}
                  onFocus={() => setSearchOpen(true)}
                />
              </Box>
            </Box>
            <Popover
              open={searchOpen && !!searchAnchor}
              anchorEl={searchAnchor}
              onClose={closeSearch}
              anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
              transformOrigin={{ vertical: "top", horizontal: "left" }}
              PaperProps={{ sx: { width: 420, mt: 1 } }}
            >
              <Box sx={{ p: 1 }}>
                {searchLoading ? (
                  <Typography fontSize={14} color="text.secondary" sx={{ p: 1 }}>
                    Buscando...
                  </Typography>
                ) : !searchResults ? (
                  <Typography fontSize={14} color="text.secondary" sx={{ p: 1 }}>
                    Digite ao menos 2 caracteres
                  </Typography>
                ) : (
                  <Box>
                    {!!searchResults.documentos?.length && (
                      <Box sx={{ mb: 1 }}>
                        <Typography fontSize={12} color="text.secondary" sx={{ px: 1, py: 0.5 }}>
                          Documentos
                        </Typography>
                        <List dense disablePadding>
                          {searchResults.documentos.map((d) => (
                            <ListItemButton
                              key={d.id}
                              onClick={() => handleNavigate("/documentos")}
                            >
                              <ListItemText
                                primary={d.nomeArquivo}
                                secondary={`${d.tipoDocumento} • ${d.statusDocumento}`}
                              />
                            </ListItemButton>
                          ))}
                        </List>
                      </Box>
                    )}

                    {!!searchResults.contratadas?.length && (
                      <Box sx={{ mb: 1 }}>
                        <Typography fontSize={12} color="text.secondary" sx={{ px: 1, py: 0.5 }}>
                          Contratadas
                        </Typography>
                        <List dense disablePadding>
                          {searchResults.contratadas.map((c) => (
                            <ListItemButton
                              key={c.id}
                              onClick={() => handleNavigate("/contratadas")}
                            >
                              <ListItemText primary={c.nome} secondary={c.cnpj} />
                            </ListItemButton>
                          ))}
                        </List>
                      </Box>
                    )}

                    {!!searchResults.funcionarios?.length && (
                      <Box sx={{ mb: 1 }}>
                        <Typography fontSize={12} color="text.secondary" sx={{ px: 1, py: 0.5 }}>
                          Funcionários
                        </Typography>
                        <List dense disablePadding>
                          {searchResults.funcionarios.map((f) => (
                            <ListItemButton
                              key={f.id}
                              onClick={() => handleNavigate("/funcionarios")}
                            >
                              <ListItemText primary={f.nomeCompleto} secondary={f.cpf} />
                            </ListItemButton>
                          ))}
                        </List>
                      </Box>
                    )}

                    {!searchResults.documentos?.length &&
                      !searchResults.contratadas?.length &&
                      !searchResults.funcionarios?.length && (
                        <Typography fontSize={14} color="text.secondary" sx={{ p: 1 }}>
                          Nenhum resultado
                        </Typography>
                      )}
                  </Box>
                )}
              </Box>
            </Popover>
            <IconButton>
              <Badge badgeContent={novosCount} color="error">
                <EmailOutlinedIcon sx={{ color: "white" }} />
              </Badge>
            </IconButton>
            <IconButton onClick={(e) => setUserMenuAnchor(e.currentTarget)}>
              <AccountCircleOutlinedIcon
                sx={{ color: "white", marginLeft: 2 }}
              />
            </IconButton>
            <Menu
              anchorEl={userMenuAnchor}
              open={!!userMenuAnchor}
              onClose={() => setUserMenuAnchor(null)}
            >
              <MenuItem disabled>
                <Box sx={{ display: "flex", flexDirection: "column" }}>
                  <Typography fontSize={12} color="text.secondary">
                    {user?.role}
                  </Typography>
                  <Typography fontSize={14}>{user?.email}</Typography>
                </Box>
              </MenuItem>
              <MenuItem onClick={handleLogout}>Sair</MenuItem>
            </Menu>

            <IconButton onClick={(e) => setThemeMenuAnchor(e.currentTarget)}>
              <SettingsOutlinedIcon sx={{ color: "white", marginLeft: 2 }} />
            </IconButton>
            <Menu
              anchorEl={themeMenuAnchor}
              open={!!themeMenuAnchor}
              onClose={() => setThemeMenuAnchor(null)}
            >
              <MenuItem selected={themeKey === "agro"} onClick={() => handleSelectTheme("agro")}>
                Tema Agro
              </MenuItem>
              <MenuItem selected={themeKey === "forest"} onClick={() => handleSelectTheme("forest")}>
                Tema Floresta
              </MenuItem>
              <MenuItem selected={themeKey === "sunset"} onClick={() => handleSelectTheme("sunset")}>
                Tema Pôr do sol
              </MenuItem>
            </Menu>
            <IconButton>
              <NotificationsNoneOutlinedIcon
                sx={{ color: "white", marginLeft: 2 }}
              />
            </IconButton>
          </Box>
        </Box>
      </Toolbar>
    </AppBar>
    // </Box>
  );
}

type SearchResponse = {
  documentos: Array<{
    id: number;
    nomeArquivo: string;
    tipoDocumento: string;
    statusDocumento: string;
  }>;
  contratadas: Array<{
    id: number;
    nome: string;
    cnpj: string;
  }>;
  funcionarios: Array<{
    id: number;
    nomeCompleto: string;
    cpf: string;
  }>;
};
