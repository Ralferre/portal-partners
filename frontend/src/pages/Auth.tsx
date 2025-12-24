import { Box, Grid, Paper } from "@mui/material";
import { ReactNode } from "react";

type AuthProps = {
  left?: ReactNode;
  right?: ReactNode;
};

export function Auth({ left, right }: AuthProps) {
  return (
    <Box
      minHeight="100vh"
      padding={4}
      sx={{
        background: "#c1c4c4ff",
      }}
    >
      <Grid container spacing={0}>
        <Grid
          minHeight="100vh"
          size={{ xs: 6, md: 6 }}
          justifyContent="center"
          alignItems="center"
          display="flex"
          sx={{
            background: "#f2f5f6ff",
          }}
        >
          <Paper
            sx={{
              maxWidth: 400,
              justifyContent: "center",
              alignItems: "center",
              background: "#f2f5f6ff",
              padding: 2,
            }}
          >
            {left}
          </Paper>
        </Grid>
        <Grid
          minHeight="100vh"
          size={{ xs: 6, md: 6 }}
          justifyContent="center"
          alignItems="center"
          display="flex"
          sx={{
            background: "linear-gradient(25deg, #0b3f4cff 0%, #3E7580 80%)",
          }}
        ></Grid>
      </Grid>
    </Box>
  );
}
