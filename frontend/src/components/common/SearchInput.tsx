import { Box, InputBase } from "@mui/material";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";

export function SearchInput() {
  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        backgroundColor: "#fff",
        borderRadius: 2,
        px: 2,
        py: 0.5,
        width: 250,
        boxShadow: "0 2px 8px rgba(0,0,0,0.05)",
      }}
    >
      <SearchOutlinedIcon sx={{ color: "text.secondary", mr: 1 }} />

      <InputBase
        placeholder="Search here"
        sx={{
          flex: 1,
          fontSize: 14,
        }}
        inputProps={{ "aria-label": "search" }}
      />
    </Box>
  );
}
