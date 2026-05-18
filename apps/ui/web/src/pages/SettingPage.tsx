import { Box, Tab, Tabs } from "@mui/material";
import { useEffect } from "react";
import { Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useTitle } from "../features/useTitle";

export default function SettingsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Settings");
  }, []);

  const tabs = [
    {
      label: "Media",
      base: "/settings/media",
      path: "/settings/media/language",
    },
    {
      label: "Processor",
      base: "/settings/processor",
      path: "/settings/processor",
    },
    {
      label: "Cleanup & Retention",
      base: "/settings/cleanup",
      path: "/settings/cleanup",
    },
  ];

  const active = tabs.findIndex((t) => location.pathname.startsWith(t.base));

  return (
    <Box
      sx={{
        height: "100%",
        width: "100%",
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
      }}
    >
      {/* Sticky header */}
      <Box
        sx={{
          borderBottom: 1,
          borderColor: "divider",
          bgcolor: "background.paper",
        }}
      >
        {location.pathname === "/settings" && (
          <Navigate to="/settings/media/language" replace />
        )}

        <Tabs
          value={active === -1 ? 0 : active}
          onChange={(e, i) => navigate(tabs[i].path)}
        >
          {tabs.map((t) => (
            <Tab key={t.path} label={t.label} />
          ))}
        </Tabs>
      </Box>

      {/* Scrollable content */}
      <Box
        sx={{
          flex: 1,
          overflow: "auto",
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
