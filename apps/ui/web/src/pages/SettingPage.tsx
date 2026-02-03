import { Box, Stack, Typography } from "@mui/material";
import { useState } from "react";
import { SettingsSidebar } from "../components/setting/SettingSidebar";
import PreferencesPage from "./settings/PreferencePage";

export default function SettingsPage() {
    const [section, setSection] = useState("preferences");

    return (
        <Stack direction="row" spacing={3} sx={{ p: 3 }}>
            <SettingsSidebar selected={section} onSelect={setSection} />

            <Box sx={{ flex: 1 }}>
                <Typography variant="h4" sx={{ mb: 2 }}>
                    Settings
                </Typography>

                {section === "preferences" && <PreferencesPage />}
            </Box>
        </Stack>
    );
}
