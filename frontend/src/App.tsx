import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import { WorkspaceProvider } from './context/WorkspaceContext'
import { Layout } from './components/Layout'
import { LoadingState } from './components/States'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { WorkspacePage } from './pages/WorkspacePage'
import { DashboardPage } from './pages/DashboardPage'
import { ProjectsPage } from './pages/ProjectsPage'
import { ProjectDetailPage } from './pages/ProjectDetailPage'
import { TaskBoardPage } from './pages/TaskBoardPage'
import { TaskDetailPage } from './pages/TaskDetailPage'
import { TeamPage } from './pages/TeamPage'
import { SettingsPage } from './pages/SettingsPage'
import { useWorkspace } from './context/WorkspaceContext'

function Protected() {
  const { user, loading } = useAuth()
  if (loading) return <LoadingState />
  return user ? <Outlet /> : <Navigate to="/login" replace />
}
function AppShell() {
  return (
    <WorkspaceProvider>
      <Outlet />
    </WorkspaceProvider>
  )
}
function WorkspaceRequired() {
  const { workspace, loading } = useWorkspace()
  if (loading) return <LoadingState />
  return workspace ? <Outlet /> : <Navigate to="/select-workspace" replace />
}
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<Protected />}>
        <Route element={<AppShell />}>
          <Route path="/select-workspace" element={<WorkspacePage />} />
          <Route element={<WorkspaceRequired />}>
            <Route element={<Layout />}>
              <Route index element={<DashboardPage />} />
              <Route path="projects" element={<ProjectsPage />} />
              <Route path="projects/:projectId" element={<ProjectDetailPage />} />
              <Route path="board" element={<TaskBoardPage />} />
              <Route path="tasks/:taskId" element={<TaskDetailPage />} />
              <Route path="team" element={<TeamPage />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
