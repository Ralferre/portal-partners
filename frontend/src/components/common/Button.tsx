import { Button as MuiButton, ButtonProps } from "@mui/material";
import { JSX, ReactNode } from "react";

type Props = ButtonProps & {
  children: React.ReactNode;
};

export function Button({ children, ...props }: Props): ReactNode {
  return (
    <MuiButton
      fullWidth
      variant="contained"
      size="large"
      sx={{ mt: 2 }}
      {...props}
    >
      {children}
    </MuiButton>
  );
}
