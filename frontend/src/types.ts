export type Role = 'ADMIN' | 'MANAGER' | 'MEMBER'
export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'IN_REVIEW' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export interface User {
  id: string
  displayName: string
  email: string
  createdAt: string
}
export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: User
}
export interface Workspace {
  id: string
  name: string
  description?: string
  currentUserRole: Role
  memberCount: number
  createdAt: string
  updatedAt: string
}
export interface Member {
  membershipId: string
  userId: string
  displayName: string
  email: string
  role: Role
  joinedAt: string
}
export interface WorkspaceInvitation {
  invitationId: string
  email: string
  role: Role
  invitedBy: User
  createdAt: string
  expiresAt: string
}
export interface InviteOrAddResponse {
  action: 'ADDED' | 'INVITED'
  member?: Member
  invitation?: WorkspaceInvitation
  invitationToken?: string
}
export interface InvitationPreview {
  email: string
  workspaceName: string
  expiresAt: string
}
export interface Project {
  id: string
  workspaceId: string
  name: string
  description?: string
  status: ProjectStatus
  owner?: User
  startDate?: string
  targetDate?: string
  totalTasks: number
  completedTasks: number
  progressPercentage: number
  createdAt: string
  updatedAt: string
}
export interface Task {
  id: string
  workspaceId: string
  projectId: string
  projectName: string
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  assignee?: User
  reporter: User
  dueDate?: string
  labels: string[]
  version: number
  createdAt: string
  updatedAt: string
}
export interface TaskPage {
  content: Task[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
export interface Comment {
  id: string
  author: User
  body: string
  createdAt: string
  updatedAt: string
}
export interface Activity {
  id: string
  type: string
  message: string
  actor: User
  createdAt: string
}
export interface TaskDetail {
  task: Task
  comments: Comment[]
  activity: Activity[]
}
export interface Dashboard {
  totalTasks: number
  tasksByStatus: Record<TaskStatus, number>
  overdueTasks: number
  tasksDueThisWeek: number
  completionPercentage: number
  projectProgress: Array<{
    projectId: string
    projectName: string
    totalTasks: number
    completedTasks: number
    percentage: number
  }>
  workload: Array<{ userId: string; displayName: string; activeTasks: number }>
  recentActivity: Activity[]
}
