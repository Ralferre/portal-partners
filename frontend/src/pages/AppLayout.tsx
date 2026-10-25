import { Box } from "@mui/material";
import { Sidebar } from "../components/common/SideBar";
import { Header } from "../components/common/Header";
import { Footer } from "../components/common/Footer";

type Props = {
  children: React.ReactNode;
};

export function AppLayout({ children }: Props) {
  return (
    <Box sx={{ display: "flex", height: "100vh" }}>
      <Sidebar />
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <Header />
        <Box
          component="main"
          sx={{
            flex: 1,
            p: 3,
            overflow: "auto",
            backgroundColor: "background.default",
          }}
        >
          {children}
        </Box>
        <Footer />
      </Box>
    </Box>
  );
}
