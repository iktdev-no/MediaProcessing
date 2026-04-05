import { Box, Button, Stack, Tab, Tabs } from "@mui/material";
import { Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { useCoordinatorPreferences } from "../../features/preferences/useCoordinatorPreferences";

import { useEffect } from "react";
import AudioTab from "../../components/settings/media-tab/AudioTab";
import LanguageTab from "../../components/settings/media-tab/LanguageTab";
import SubtitleTab from "../../components/settings/media-tab/SubtitleTab";
import VideoTab from "../../components/settings/media-tab/VideoTab";
import { useTitle } from "../../features/useTitle";

export default function MediaPreferencesPage() {
  const { prefs, setPrefs, save, reset, isDirty, loading } =
    useCoordinatorPreferences();
  const navigate = useNavigate();
  const location = useLocation();

  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Settings - Media");
  }, []);

  if (loading || !prefs) return <div>Laster…</div>;

  const tabs = [
    {
      label: "Language",
      path: "language",
      component: <LanguageTab prefs={prefs} setPrefs={setPrefs} />,
    },
    {
      label: "Subtitles",
      path: "subtitles",
      component: <SubtitleTab prefs={prefs} setPrefs={setPrefs} />,
    },
    {
      label: "Audio",
      path: "audio",
      component: <AudioTab prefs={prefs} setPrefs={setPrefs} />,
    },
    {
      label: "Video",
      path: "video",
      component: <VideoTab prefs={prefs} setPrefs={setPrefs} />,
    },
  ];

  // Finn aktiv tab basert på URL
  const activeIndex = tabs.findIndex((t) => location.pathname.endsWith(t.path));

  return (
    <Box
      sx={{
        height: "100%",
        width: "100%",
        display: "flex",
        flexDirection: "row",
        overflow: "hidden",
      }}
    >
      {/* Tabs */}
      <Box
        sx={{
          width: 180,
          flexShrink: 0,
          overflowY: "auto",
          borderRight: 1,
          borderColor: "divider",
        }}
      >
        <Tabs
          orientation="vertical"
          value={activeIndex === -1 ? 0 : activeIndex}
          onChange={(e, i) => navigate(`/settings/media/${tabs[i].path}`)}
          variant="scrollable"
          scrollButtons="auto"
        >
          {tabs.map((t, i) => (
            <Tab key={i} label={t.label} />
          ))}
        </Tabs>
      </Box>

      {/* Content */}
      <Box
        sx={{
          flex: 1,
          minWidth: 0,
          height: "100%",
          overflowY: "auto",
          pl: 3,
          pr: 3,
          pt: 2,
          pb: 4,
        }}
      >
        <Routes>
          <Route
            index
            element={<LanguageTab prefs={prefs} setPrefs={setPrefs} />}
          />

          {tabs.map((t) => (
            <Route key={t.path} path={t.path} element={t.component} />
          ))}
        </Routes>

        <Stack direction="row" spacing={2} sx={{ mt: 4 }}>
          <Button variant="contained" disabled={!isDirty} onClick={save}>
            Save
          </Button>
          <Button variant="outlined" onClick={reset}>
            Reset
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}
