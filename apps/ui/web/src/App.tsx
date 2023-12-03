import React, { useEffect } from 'react';
import logo from './logo.svg';
import './App.css';
import { Box, CssBaseline } from '@mui/material';
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
  

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{
        height: 70,
        backgroundColor: theme.palette.action.selected
      }}>
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
              <Route path='/files' element={<ExplorePage />} />
              <Route path='/' element={<LaunchPage />} />
            </Routes>
          <Footer />
        </BrowserRouter>
      </Box>
    </ThemeProvider>
  );
}

export default App;
