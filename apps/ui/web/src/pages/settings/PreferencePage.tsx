import { Box, Button, Stack, Tab, Tabs } from "@mui/material";
import { useState } from "react";
import { usePreferences } from "../../features/preferences/usePreferences";

import { AudioTab } from "../../components/preferences/tabs/AudioTab";
import { LanguageTab } from "../../components/preferences/tabs/LanguageTab";
import { SubtitleTab } from "../../components/preferences/tabs/SubtitleTab";
import { VideoTab } from "../../components/preferences/tabs/VideoTab";

export default function PreferencesPage() {
    const { prefs, setPrefs, save, reset, isDirty, loading } = usePreferences();
    const [tab, setTab] = useState(0);

    if (loading || !prefs) return <div>Laster…</div>;

    const tabs = [
        { label: "Language", component: <LanguageTab prefs={prefs} setPrefs={setPrefs} /> },
        { label: "Subtitles", component: <SubtitleTab prefs={prefs} setPrefs={setPrefs} /> },
        { label: "Audio", component: <AudioTab prefs={prefs} setPrefs={setPrefs} /> },
        { label: "Video", component: <VideoTab prefs={prefs} setPrefs={setPrefs} /> },
    ];

    return (
        <Stack direction="row" spacing={4} sx={{ p: 3 }}>
            {/* Vertical Tabs */}
            <Tabs
                orientation="vertical"
                value={tab}
                onChange={(_, v) => setTab(v)}
                sx={{ borderRight: 1, borderColor: "divider", minWidth: 180 }}
            >
                {tabs.map((t, i) => (
                    <Tab key={i} label={t.label} />
                ))}
            </Tabs>

            {/* Content */}
            <Box sx={{ flex: 1 }}>
                {tabs[tab].component}

                <Stack direction="row" spacing={2} sx={{ mt: 4 }}>
                    <Button variant="contained" disabled={!isDirty} onClick={save}>
                        Save
                    </Button>
                    <Button variant="outlined" onClick={reset}>
                        Reset
                    </Button>
                </Stack>
            </Box>
        </Stack>
    );
}
