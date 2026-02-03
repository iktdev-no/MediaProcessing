import { useState } from 'react'
import './App.css'

import { Box } from "@mui/material"
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ToastContainer } from 'react-toastify'
import { Sidebar } from "./components/Sidebar"
import { TopBar } from "./components/TopBar"
import { HealthProvider } from './context/HealthProvider'
import DashboardPage from './pages/DasboardPage'
import EventsPage from './pages/EventsPage'
import EventsSequencePage from './pages/EventsSequencePage'
import FilesPage from './pages/FilesPage'
import HealthPage from './pages/HealthPage'
import { SequencePage } from './pages/SequencePage'
import SettingPage from './pages/SettingPage'
import TasksPage from './pages/TasksPage'

interface AppLayoutProps {
  children: React.ReactNode
}

export function AppLayout({ children }: AppLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const drawerWidth = sidebarOpen ? 260 : 64

  return (
    <Box
      sx={{
        display: "flex",
        width: "100vw",     // ← kritisk
        height: "100vh",    // ← kritisk
        overflow: "hidden"  // ← hindrer scroll her
      }}
    >
      <TopBar onToggleSidebar={() => setSidebarOpen(prev => !prev)} />

      <Sidebar open={sidebarOpen} />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          ml: `${drawerWidth}px`,
          mt: "64px",
          width: `calc(100vw - ${drawerWidth}px)`, // ← kritisk
          height: `calc(100vh - 56px)`,            // ← kritisk
          overflow: "hidden",                      // ← main skal ikke scrolle
          position: "relative"                     // ← for sticky i child
        }}
      >
        {children}
      </Box>
    </Box>
  )
}




function App() {
  return (
    <BrowserRouter>
      <HealthProvider>
        <AppLayout>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/setting" element={<SettingPage />} />
            <Route path='/sequences' element={<SequencePage />} />
            <Route path="/files" element={<FilesPage />} />
            <Route path="/health" element={<HealthPage />} />
            <Route path="/tasks" element={<TasksPage />} />
            <Route path="/events" element={<EventsPage />} />
            <Route path="/events/sequence/:referenceId" element={<EventsSequencePage />} />
          </Routes>
          <ToastContainer
            position='bottom-left'
            autoClose={3000}
            hideProgressBar={true}
            newestOnTop={true}
            closeOnClick
            pauseOnHover
            theme='dark'
          />
        </AppLayout>
      </HealthProvider>
    </BrowserRouter>
  )
}


export default App
