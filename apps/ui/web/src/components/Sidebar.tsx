import {
    Box,
    Drawer,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText
} from "@mui/material"
import { type JSX } from "react"
import { useSidebarMenu } from "../menu/sidebar-menu-items"
import type { MenuItem } from "../types/MenuItem"

type SidebarProps = {
    open?: boolean
    topOffset?: number
}


export function Sidebar({ open, topOffset = 64 }: SidebarProps): JSX.Element {
    const { topMenu, bottomMenu } = useSidebarMenu()
    const drawerWidth = open ? 260 : 64

    return (
        <Drawer
            variant="permanent"
            PaperProps={{
                sx: {
                    width: drawerWidth,
                    overflowX: "hidden",
                    whiteSpace: "nowrap",
                    transition: (theme) =>
                        theme.transitions.create("width", {
                            easing: theme.transitions.easing.sharp,
                            duration: theme.transitions.duration.standard
                        }),
                    top: `${topOffset}px`,
                    height: `calc(100% - ${topOffset}px)`
                }
            }}
        >
            <List>
                {topMenu.map((item: MenuItem) => (
                    <ListItemButton
                        key={item.id}
                        onClick={item.onClick}
                        sx={{
                            height: 48,
                            px: open ? 2.5 : 2,          // ← matcher hamburger offset
                            justifyContent: open ? "initial" : "flex-start"
                        }}
                    >
                        <ListItemIcon
                            sx={{
                                minWidth: 0,
                                mr: open ? 2 : 0,
                                justifyContent: "flex-start",   // ← viktig
                                pl: open ? 0 : 0.5               // ← finjustering
                            }}
                        >

                            {item.icon}
                        </ListItemIcon>

                        {open && (
                            <ListItemText
                                primary={item.label}
                                sx={{
                                    m: 0
                                }}
                            />
                        )}
                    </ListItemButton>

                ))}
            </List>

            {bottomMenu.length > 0 && (
                <>
                    <Box sx={{ flexGrow: 1 }} />
                    <List>
                        {bottomMenu.map((item: MenuItem) => (
                            <ListItemButton
                                key={item.id}
                                onClick={item.onClick}
                                sx={{
                                    height: 48,
                                    px: open ? 2.5 : 2,          // ← matcher hamburger offset
                                    justifyContent: open ? "initial" : "flex-start"
                                }}
                            >
                                <ListItemIcon
                                    sx={{
                                        minWidth: 0,
                                        mr: open ? 2 : 0,
                                        justifyContent: "flex-start",   // ← viktig
                                        pl: open ? 0 : 0.5               // ← finjustering
                                    }}
                                >

                                    {item.icon}
                                </ListItemIcon>

                                {open && (
                                    <ListItemText
                                        primary={item.label}
                                        sx={{
                                            m: 0
                                        }}
                                    />
                                )}
                            </ListItemButton>

                        ))}
                    </List>
                </>
            )}


        </Drawer>
    )
}
