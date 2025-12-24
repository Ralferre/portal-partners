import { Box, Divider, Grid, Paper, Typography } from "@mui/material";
import { ReactNode } from "react";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import QueryStatsOutlinedIcon from "@mui/icons-material/QueryStatsOutlined";
import ThumbUpAltOutlinedIcon from "@mui/icons-material/ThumbUpAltOutlined";
import ThumbDownOffAltOutlinedIcon from "@mui/icons-material/ThumbDownOffAltOutlined";

type CardInfo = {
  title: string;
  value: number;
  icon: ReactNode;
  color: string;
};

const cardList = [
  {
    title: "Postados",
    value: 28,
    icon: <EmailOutlinedIcon />,
    color: "#007bff",
  },
  {
    title: "Analisados",
    value: 15,
    icon: <QueryStatsOutlinedIcon />,
    color: "#ffc107",
  },
  {
    title: "Aprovados",
    value: 10,
    icon: <ThumbUpAltOutlinedIcon />,
    color: "#28a745",
  },
  {
    title: "Reprovados",
    value: 5,
    icon: <ThumbDownOffAltOutlinedIcon />,
    color: "#dc3545",
  },
];

export function AppCard() {
  return (
    <Grid container spacing={3}>
      {cardList.map((card) => (
        <Grid key={card.title} size={{ xs: 12, sm: 6, md: 3 }}>
          <Paper
            sx={{
              p: 2,
              borderRadius: 3,
              boxShadow: "0px 10px 30px rgba(0,0,0,0.08)",
            }}
          >
            <Box
              sx={{
                width: 48,
                height: 48,
                borderRadius: 2,
                backgroundColor: card.color,
                color: "#fff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                mb: 2,
              }}
            >
              {card.icon}
            </Box>
            <Divider orientation="horizontal" variant="middle" flexItem />
            <Typography variant="body1" align="center" fontWeight={700}>
              {card.title}
            </Typography>
            <Typography variant="h5" align="center" color="text.secondary">
              {card.value}
            </Typography>
          </Paper>
        </Grid>
      ))}
    </Grid>
  );
}
