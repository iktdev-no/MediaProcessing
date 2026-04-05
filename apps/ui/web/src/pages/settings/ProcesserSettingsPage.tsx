// ProcessorPreferencesPage.tsx
import { Box, Tab, Tabs } from "@mui/material";
import { Route, Routes, useLocation, useNavigate } from "react-router-dom";
import LimitTab from "../../components/settings/processer-tab/LimitTab";

export default function ProcessorPreferencesPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const tabs = [
    {
      label: "Limit",
      path: "limit",
      component: <LimitTab />,
    },
  ];

  const activeIndex = tabs.findIndex((t) => location.pathname.endsWith(t.path));

  return (
    <Box
      sx={{
        height: "100%",
        width: "100%",
        display: "flex",
        overflow: "hidden",
      }}
    >
      {/* Tabs */}
      <Box
        sx={{
          width: 180,
          borderRight: 1,
          borderColor: "divider",
          overflowY: "auto",
        }}
      >
        <Tabs
          orientation="vertical"
          value={activeIndex === -1 ? 0 : activeIndex}
          onChange={(e, i) => navigate(`/settings/processor/${tabs[i].path}`)}
        >
          {tabs.map((t, i) => (
            <Tab key={i} label={t.label} />
          ))}
        </Tabs>
      </Box>

      {/* Content */}
      <Box sx={{ flex: 1, minWidth: 0, overflowY: "auto", p: 3 }}>
        <Routes>
          <Route index element={tabs[0].component} />
          {tabs.map((t) => (
            <Route key={t.path} path={t.path} element={t.component} />
          ))}
        </Routes>
      </Box>
    </Box>
  );
}
