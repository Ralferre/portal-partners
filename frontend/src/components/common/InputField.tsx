import {
  FormControl,
  FormControlProps,
  InputAdornment,
  InputLabel,
  OutlinedInput,
  FormHelperText,
} from "@mui/material";
import { ReactNode, JSX } from "react";

type CustomOutlinedInputProps = {
  startIcon?: ReactNode;
  endIcon?: ReactNode;
  onClick?: ReactNode;
  label: string;
  type?: string;
  error?: boolean;
  helperText?: string;
} & Omit<FormControlProps, "onChange"> & {
    value?: string | number;
    onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  };

export function InputField({
  label,
  type = "text",
  startIcon,
  endIcon,
  error,
  helperText,
  value,
  onChange,
  ...inputProps
}: CustomOutlinedInputProps): JSX.Element {
  const id = `outlined-input-${label.replace(/\s+/g, "-").toLowerCase()}`;

  return (
    <FormControl
      fullWidth
      variant="outlined"
      margin="dense"
      {...inputProps}
      error={error}
    >
      <InputLabel htmlFor={id}>{label}</InputLabel>

      <OutlinedInput
        id={id}
        type={type}
        label={label}
        value={value}
        onChange={onChange}
        startAdornment={
          startIcon ? (
            <InputAdornment position="start">{startIcon}</InputAdornment>
          ) : undefined
        }
        endAdornment={
          endIcon ? (
            <InputAdornment position="end">{endIcon}</InputAdornment>
          ) : undefined
        }
        required
      />
      {helperText ? <FormHelperText>{helperText}</FormHelperText> : null}
    </FormControl>
  );
}
