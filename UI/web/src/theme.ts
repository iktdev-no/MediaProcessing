import { createTheme  } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: {
      main: '#8800da',
    },
    secondary: {
      main: '#004dbb',
    },
    background: {
      default: '#181818',
      paper: '#080808',
    },
    divider: '#000000',
    action: {
      selected: "rgb(67 0 107)"
    }
  },
});

export default theme;