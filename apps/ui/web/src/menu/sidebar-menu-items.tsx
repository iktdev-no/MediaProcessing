import AutoAwesomeMotionIcon from "@mui/icons-material/AutoAwesomeMotion";
import DashboardIcon from "@mui/icons-material/Dashboard";
import FolderIcon from "@mui/icons-material/Folder";
import InboxIcon from "@mui/icons-material/Inbox";
import MonitorHeartIcon from "@mui/icons-material/MonitorHeart";
import SettingsIcon from "@mui/icons-material/Settings";
import SubscriptionsIcon from "@mui/icons-material/Subscriptions";
import { useNavigate } from "react-router-dom";

export function useSidebarMenu() {
  const navigate = useNavigate();
  return {
    topMenu: [
      {
        id: "dashboard",
        label: "Dashboard",
        icon: <DashboardIcon />,
        onClick: () => navigate("/"),
      },
      {
        id: "sequences",
        label: "Sequences",
        icon: <InboxIcon />,
        onClick: () => navigate("/sequences"),
      },
      {
        id: "events",
        label: "Events",
        icon: <SubscriptionsIcon />,
        onClick: () => navigate("/events"),
      },
      {
        id: "tasks",
        label: "Tasks",
        icon: <AutoAwesomeMotionIcon />,
        onClick: () => navigate("/tasks"),
      },
      {
        id: "Files",
        label: "Files",
        icon: <FolderIcon />,
        onClick: () => navigate("/files"),
      },
    ],
    bottomMenu: [
      {
        id: "health",
        label: "Health",
        icon: <MonitorHeartIcon />,
        onClick: () => navigate("/health"),
      },
      {
        id: "settings",
        label: "Settings",
        icon: <SettingsIcon />,
        onClick: () => navigate("/settings"),
      },
    ],
  };
}
