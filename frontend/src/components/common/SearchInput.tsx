import React from "react";
import { Box, InputBase } from "@mui/material";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";

type Props = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  onFocus?: () => void;
  onBlur?: () => void;
};

export const SearchInput = React.forwardRef<HTMLDivElement, Props>(function SearchInput(
  { value, onChange, placeholder = "Buscar...", onFocus, onBlur }: Props,
  ref
) {
  return (
    <Box
      ref={ref}
      sx={{
        display: "flex",
        alignItems: "center",
        backgroundColor: "#fff",
        borderRadius: 2,
        px: 2,
        py: 0.5,
        width: 320,
        boxShadow: "0 2px 8px rgba(0,0,0,0.05)",
      }}
    >
      <SearchOutlinedIcon sx={{ color: "text.secondary", mr: 1 }} />

      <InputBase
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={onFocus}
        onBlur={onBlur}
        placeholder={placeholder}
        sx={{
          flex: 1,
          fontSize: 14,
        }}
        inputProps={{ "aria-label": "search" }}
      />
    </Box>
  );
});
