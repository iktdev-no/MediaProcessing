import React, { useEffect, useState } from 'react';
import logo from './logo.svg';
import './App.css';
import { Box, CssBaseline, IconButton, SxProps, Theme } from '@mui/material';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Footer from './app/features/footer';
import LaunchPage from './app/page/LaunchPage';
import { useWsSubscription } from './app/ws/subscriptions';
import { useDispatch } from 'react-redux';
import { useStompClient, useSubscription } from 'react-stomp-hooks';
import { updateItems } from './app/store/composed-slice';
import ExplorePage from './app/page/ExplorePage';
import { ThemeProvider } from '@mui/material';
import theme from './theme';
import { simpleEventsUpdate } from './app/store/kafka-items-flat-slice';
import { EventDataObject, SimpleEventDataObject } from './types';
import EventsChainPage from './app/page/EventsChainPage';
import UnprocessedFilesPage from './app/page/UnprocessedFilesPage';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import FolderIcon from '@mui/icons-material/Folder';
import QueueIcon from '@mui/icons-material/Queue';
import AppsIcon from '@mui/icons-material/Apps';
import ConstructionIcon from '@mui/icons-material/Construction';
import ProcesserTasksPage from './app/page/ProcesserTasksPage';

function App() {
  const client = useStompClient();
  const dispatch = useDispatch();
  
  useWsSubscription<Array<EventDataObject>>("/topic/event/items", (response) => {
    dispatch(updateItems(response))
  });

  useWsSubscription<Array<SimpleEventDataObject>>("/topic/event/flat", (response) => {
    dispatch(simpleEventsUpdate(response))
  });


  useEffect(() => {
    // Kjør din funksjon her når komponenten lastes inn for første gang
    // Sjekk om cursor er null

      // Kjør din funksjon her når cursor er null og client ikke er null
      client?.publish({
        destination: "/app/items",
        body: undefined
      })
  
      // Alternativt, du kan dispatche en Redux handling her
      // dispatch(fetchDataAction()); // Eksempel på å dispatche en handling
    
  }, [client, dispatch]);
  
  const iconHeight: SxProps<Theme> = {
    height: 50,
    width: 50
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{
        height: 70,
        display: "flex",
        alignItems: "center",
        paddingLeft: 1,
        backgroundColor: theme.palette.action.selected
      }}>
        <IconButton onClick={() => window.location.href = "/"} sx={{
          ...iconHeight
        }}>
          <AppsIcon />
        </IconButton>
        <IconButton onClick={() => window.location.href = "/events"} sx={{
          ...iconHeight
        }}>
          <AccountTreeIcon />
        </IconButton>
        <IconButton onClick={() => window.location.href = "/files"} sx={{
          ...iconHeight
        }}>
          <FolderIcon />
        </IconButton>
        <IconButton onClick={() => window.location.href = "/unprocessed"} sx={{
          ...iconHeight
        }}>
          <QueueIcon />
        </IconButton>
        <IconButton onClick={() => window.location.href = "/tasks"} sx={{
          ...iconHeight
        }}>
          <ConstructionIcon />
        </IconButton>
      </Box>
      <Box sx={{
        display: "block",
        maxHeight: window.screen.height - 70,
        height: window.screen.height - 70,
        width: "100vw",
        maxWidth: "100vw"
      }}>
        <BrowserRouter>
            <Routes>
              <Route path='/tasks' element={<ProcesserTasksPage />} />
              <Route path='/unprocessed' element={<UnprocessedFilesPage />} />
              <Route path='/files' element={<ExplorePage />} />
              <Route path='/events' element={<EventsChainPage />} />
              <Route path='/' element={<LaunchPage />} />
            </Routes>
          <Footer />
        </BrowserRouter>
      </Box>
    </ThemeProvider>
  );
}

export default App;
