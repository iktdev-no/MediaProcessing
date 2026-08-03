import MenuIcon from "@mui/icons-material/Menu";
import { AppBar, Box, IconButton, keyframes, Toolbar, Typography } from "@mui/material";
import { useTitle } from "../../features/useTitle";
import { useSseSelector } from "../../sse/useSseSelector";
import { getSystemHealthState } from "../../util";
import type { SSEConnectionState } from "../../sse/state";
import RadioButtonCheckedIcon from '@mui/icons-material/RadioButtonChecked';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';

interface TopBarProps {
  onToggleSidebar: () => void;
}


const pulseAnimation = keyframes`
  0% { opacity: 1; }
  50% { opacity: 0.3; }
  100% { opacity: 1; }
`;

function SSEConnectionBadge({ state }: { state: SSEConnectionState }) {
  switch (state) {
    case "Connected":
      return <RadioButtonCheckedIcon color="success" />;

    case "Connecting":
      return (
        <RadioButtonCheckedIcon
          sx={{
            color: "warning.main", // Oransje/gul farge
            animation: `${pulseAnimation} 1.2s infinite ease-in-out`
          }}
        />
      );

    case "Disconnected":
      return <RadioButtonUncheckedIcon color="error" />;
  }
  return null;
}

export function TopBar({ onToggleSidebar }: TopBarProps) {
  const { title } = useTitle();
  const status = useSseSelector(state => getSystemHealthState(state.systemHealth));
  const sseConnection = useSseSelector(state => state.sseState)
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

        <Box sx={{ display: "flex", flexDirection: "row", alignItems: "center", gap: 1 }}>
          <SSEConnectionBadge state={sseConnection} />

          <Box
            sx={{
              px: 2,
              py: 0.5,
              borderRadius: 1,
              bgcolor:
                status === "HEALTHY"
                  ? "success.main"
                  : status === "UNHEALTHY"
                    ? "error.main"
                    : status === "DEGRADED"
                      ? "warning.main"
                      : "grey.600",
              textTransform: "capitalize",
            }}
          >
            {status}
          </Box>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
