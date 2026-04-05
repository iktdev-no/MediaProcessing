import MenuIcon from "@mui/icons-material/Menu";
import { AppBar, Box, IconButton, Toolbar, Typography } from "@mui/material";
import { useHealth } from "../context/HealthProvider";
import { useTitle } from "../features/useTitle";

interface TopBarProps {
  onToggleSidebar: () => void;
}

export function TopBar({ onToggleSidebar }: TopBarProps) {
  const { title } = useTitle();
  const { status } = useHealth();
  return (
    <AppBar position="fixed" sx={{ zIndex: 1201 }}>
      <Toolbar>
        <IconButton
          edge="start"
          color="inherit"
          onClick={onToggleSidebar}
          sx={{ mr: 2 }}
        >
          <MenuIcon />
        </IconButton>

        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          {title}
        </Typography>

        <Box
          sx={{
            px: 2,
            py: 0.5,
            borderRadius: 1,
            bgcolor:
              status === "healthy"
                ? "success.main"
                : status === "unhealthy"
                  ? "error.main"
                  : status === "reconnecting"
                    ? "warning.main"
                    : "grey.600",
            textTransform: "capitalize",
          }}
        >
          {status}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
