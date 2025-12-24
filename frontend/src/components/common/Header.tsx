import {
  AppBar,
  Badge,
  Box,
  IconButton,
  Toolbar,
  Typography,
} from "@mui/material";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import { SearchInput } from "./SearchInput";

export function Header() {
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
                <SearchInput />
              </Box>
            </Box>
            <IconButton>
              <Badge badgeContent={8} color="error">
                <EmailOutlinedIcon sx={{ color: "white" }} />
              </Badge>
            </IconButton>
            <IconButton>
              <AccountCircleOutlinedIcon
                sx={{ color: "white", marginLeft: 2 }}
              />
            </IconButton>
            <IconButton>
              <SettingsOutlinedIcon sx={{ color: "white", marginLeft: 2 }} />
            </IconButton>
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
