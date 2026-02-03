import { List, ListItemButton, ListItemText, Paper } from "@mui/material";

type Props = {
    selected: string;
    onSelect: (id: string) => void;
};

export function SettingsSidebar({ selected, onSelect }: Props) {
    const items = [
        { id: "preferences", label: "Media Preferences" },
        // legg til flere senere
    ];

    return (
        <Paper sx={{ width: 220, p: 1 }}>
            <List>
                {items.map((item) => (
                    <ListItemButton
                        key={item.id}
                        selected={selected === item.id}
                        onClick={() => onSelect(item.id)}
                    >
                        <ListItemText primary={item.label} />
                    </ListItemButton>
                ))}
            </List>
        </Paper>
    );
}
