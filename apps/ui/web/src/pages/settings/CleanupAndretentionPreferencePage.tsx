import { Box, Button, Stack, Tab, Tabs } from "@mui/material";
import { useEffect, useState } from "react";
import { Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { getUsedFiles, putPreservedFiles } from "../../api/coordinator/files";
import CleanupRetentionTab from "../../components/settings/cleanup-tab/CleanupRetentionTab";
import FilePreservationTab from "../../components/settings/cleanup-tab/FilePreservationTab";
import { useCoordinatorPreferences } from "../../features/preferences/useCoordinatorPreferences";
import { useTitle } from "../../features/useTitle";
import type { InputFileInfo } from "../../types/transfer-model";

export default function CleanupAndRetentionPreferencesPage() {
  const {
    prefs,
    setPrefs,
    save: savePrefs,
    reset: resetPrefs,
    isDirty: isPrefsDirty,
    loading: prefsLoading,
  } = useCoordinatorPreferences();

  const navigate = useNavigate();
  const location = useLocation();
  const { setTitle } = useTitle();

  // State for File Preservation
  const [files, setFiles] = useState<InputFileInfo[]>([]);
  const [initialFiles, setInitialFiles] = useState<InputFileInfo[]>([]);
  const [filesLoading, setFilesLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setTitle("Settings - Cleanup & Retention");

    async function loadFiles() {
      setFilesLoading(true);
      try {
        const data = await getUsedFiles();
        setFiles(data);
        setInitialFiles(data);
      } catch (error) {
        console.error("Kunne ikke laste filer", error);
      } finally {
        setFilesLoading(false);
      }
    }

    loadFiles();
  }, []);

  const isLoading = prefsLoading || filesLoading;

  // 1. GURAD CLAUSE: Returner tidlig hvis vi laster eller mangler data.
  // Dette garanterer at alt under (inkludert tabs-arrayet) har tilgang til gyldig data.
  if (isLoading || !prefs) {
    return <div>Laster…</div>;
  }

  // 2. Nå er vi 100% sikre på at prefs IKKE er null
  const tabs = [
    {
      label: "Cleanup & Retention",
      path: "main",
      component: <CleanupRetentionTab prefs={prefs} setPrefs={setPrefs} />,
    },
    {
      label: "File Preservation",
      path: "preservation",
      component: <FilePreservationTab files={files} setFiles={setFiles} />,
    },
  ];

  const activeIndex = tabs.findIndex((t) => location.pathname.endsWith(t.path));
  const currentTabSafeIndex = activeIndex === -1 ? 0 : activeIndex;

  // Sjekk "dirty"-status kun for aktiv tab
  const isFilesDirty = JSON.stringify(files) !== JSON.stringify(initialFiles);
  const isCurrentTabDirty =
    currentTabSafeIndex === 0 ? isPrefsDirty : isFilesDirty;

  // Reset kun den aktive taben
  const handleReset = () => {
    if (currentTabSafeIndex === 0) {
      resetPrefs();
    } else {
      setFiles(initialFiles);
    }
  };

  // Lagre kun den aktive taben
  const handleSave = async () => {
    setSaving(true);
    try {
      if (currentTabSafeIndex === 0 && isPrefsDirty) {
        await savePrefs();
      } else if (currentTabSafeIndex === 1 && isFilesDirty) {
        const preservedUris = files
          .filter((f) => f.preserved)
          .map((f) => f.fileUri);
        const updated = await putPreservedFiles(preservedUris);
        setFiles(updated);
        setInitialFiles(updated);
      }
    } catch (error) {
      console.error("Lagring feilet", error);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box sx={{ height: "100%", width: "100%", display: "flex" }}>
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
          value={currentTabSafeIndex}
          onChange={(e, i) => navigate(`/settings/cleanup/${tabs[i].path}`)}
        >
          {tabs.map((t, i) => (
            <Tab key={i} label={t.label} />
          ))}
        </Tabs>
      </Box>

      {/* Content */}
      <Box
        sx={{ flex: 1, minWidth: 0, height: "100%", overflowY: "auto", p: 3 }}
      >
        <Routes>
          <Route
            index
            element={<CleanupRetentionTab prefs={prefs} setPrefs={setPrefs} />}
          />
          {tabs.map((t) => (
            <Route key={t.path} path={t.path} element={t.component} />
          ))}
        </Routes>

        {/* Globale knapper */}
        <Stack direction="row" spacing={2} sx={{ mt: 4 }}>
          <Button
            variant="contained"
            disabled={!isCurrentTabDirty || saving}
            onClick={handleSave}
          >
            {saving ? "Saving..." : "Save"}
          </Button>
          <Button
            variant="outlined"
            disabled={!isCurrentTabDirty || saving}
            onClick={handleReset}
          >
            Reset
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}
