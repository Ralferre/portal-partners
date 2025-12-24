import { Box } from "@mui/material";
import { Sidebar } from "../components/common/SideBar";
import { Header } from "../components/common/Header";
import { Outlet } from "react-router-dom";

type Props = {
  children: React.ReactNode;
};

export function AppLayout({ children }: Props) {
  return (
    <Box sx={{ display: "flex", height: "100vh" }}>
      <Sidebar />
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <Header />
        <Box p={3}>
          <Outlet />
        </Box>
        <Box
          component="main"
          sx={{
            flex: 1,
            p: 3,
            overflow: "auto",
            backgroundColor: "#f5f6fa",
          }}
        >
          {children}
        </Box>
      </Box>
    </Box>
  );
}
