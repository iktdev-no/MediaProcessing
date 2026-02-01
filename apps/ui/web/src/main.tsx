import { CssBaseline } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { SseProvider } from './sse/SseProvider';
import { ThemeProvider } from "./theme/ThemeContext";

const queryClient = new QueryClient()

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <ThemeProvider>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <SseProvider>
          <App />
        </SseProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </React.StrictMode>
)
