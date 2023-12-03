import React, { useState } from 'react';
import { styled } from '@mui/material/styles';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import InboxIcon from '@mui/icons-material/Inbox';
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks';

const drawerWidth = 240;

const SidebarWrapper = styled('div')({
  display: 'flex',
});

const DrawerWrapper = styled(Drawer)({
  width: drawerWidth,
  flexShrink: 0,
});

const DrawerPaperWrapper = styled('div')({
  width: drawerWidth,
});

const ContentWrapper = styled('div')({
  flexGrow: 1,
  padding: '16px',
});

export default function Sidebar() {
  const [open, setOpen] = useState(true);

  const handleToggle = () => {
    setOpen(!open);
  };

  return (
    <SidebarWrapper>
      <DrawerWrapper
        variant="persistent"
        anchor="left"
        open={open}
        PaperProps={{
          sx: { width: drawerWidth },
        }}
      >
        <DrawerPaperWrapper />
        <List>
          <ListItem button>
            <ListItemIcon>
              <InboxIcon />
            </ListItemIcon>
            <ListItemText primary="Incoming" />
          </ListItem>
          <ListItem button>
            <ListItemIcon>
              <LibraryBooksIcon />
            </ListItemIcon>
            <ListItemText primary="Library" />
          </ListItem>
        </List>
      </DrawerWrapper>
      <ContentWrapper>
        {/* Main content */}
      </ContentWrapper>
    </SidebarWrapper>
  );
}
