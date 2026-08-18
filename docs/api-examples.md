# API examples

The API is rooted at `http://localhost:8080/api`. Swagger UI is available at `http://localhost:8080/swagger-ui.html`. Replace shell variables with literal values on shells that do not support this syntax.

## Authenticate

```bash
curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@taskflow.local","password":"Admin123!"}'
```

Copy `accessToken` from the response:

```bash
TOKEN='<accessToken>'
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

## Create and inspect a workspace

```bash
curl -s http://localhost:8080/api/workspaces \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Launch team","description":"Ship the first release"}'

WORKSPACE_ID='<workspace UUID>'
curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/members" \
  -H "Authorization: Bearer $TOKEN"
```

## Invite, change roles, and remove members

Admins use one endpoint for both cases. A registered email is added immediately. An unregistered email creates a pending invitation and returns an opaque, seven-day registration token. The React app turns that token into a copyable link; share the link through your normal team channel. The registration screen validates the link and locks the invited email, so the recipient does not need to guess or type it.

```bash
curl -s -X POST "http://localhost:8080/api/workspaces/$WORKSPACE_ID/members/invite-or-add" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"new.teammate@example.com","role":"MEMBER"}'

curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/members/invitations" \
  -H "Authorization: Bearer $TOKEN"
```

The invitation response contains `invitationToken` only at creation or regeneration time. Build the shareable local link as `http://localhost:3000/register?invite=<invitationToken>`; a hosted site uses its own public origin. The database stores only a SHA-256 hash, and an admin can use **Copy new link** to invalidate the old token and create another one.

Admins can cancel a pending invitation, update a member role, or remove a member. The API prevents removal or demotion of the final workspace admin.

```bash
INVITATION_ID='<invitation UUID>'
MEMBERSHIP_ID='<membership UUID>'
curl -s -X DELETE "http://localhost:8080/api/workspaces/$WORKSPACE_ID/members/invitations/$INVITATION_ID" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X PATCH "http://localhost:8080/api/workspaces/$WORKSPACE_ID/members/$MEMBERSHIP_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"role":"MANAGER"}'
```

## Create a project and task

```bash
curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/projects" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Public beta","description":"Beta milestone","status":"ACTIVE","startDate":"2026-08-17","targetDate":"2026-09-30"}'

PROJECT_ID='<project UUID>'
curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"projectId\":\"$PROJECT_ID\",\"title\":\"Verify release\",\"description\":\"Run the release checklist\",\"status\":\"TODO\",\"priority\":\"HIGH\",\"dueDate\":\"2026-09-20\",\"labels\":[\"release\"]}"
```

## Filter and page tasks

```bash
curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/tasks?status=TODO&priority=HIGH&search=release&page=0&size=20&sort=dueDate&direction=asc" \
  -H "Authorization: Bearer $TOKEN"
```

Additional filters are `assigneeId`, `projectId`, `dueAfter`, and `dueBefore`. Allowed sort fields are validated by the backend.

## Optimistic task update

Read the task first, then include its current `version` in the update. Concurrent stale versions return `409 Conflict`.

```bash
TASK_ID='<task UUID>'
curl -s -X PUT "http://localhost:8080/api/workspaces/$WORKSPACE_ID/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"projectId\":\"$PROJECT_ID\",\"title\":\"Verify release\",\"description\":\"Checklist complete\",\"status\":\"DONE\",\"priority\":\"HIGH\",\"labels\":[\"release\"],\"version\":0}"
```

## Comment and dashboard

```bash
curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/tasks/$TASK_ID/comments" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"body":"Release checks completed."}'

curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/dashboard" \
  -H "Authorization: Bearer $TOKEN"
```

## Admin login history

Workspace admins can review successful sign-ins by workspace members. The endpoint returns the account and timestamp only; it does not store passwords, IP addresses, or browser-tracking data.

```bash
curl -s "http://localhost:8080/api/workspaces/$WORKSPACE_ID/security/login-history?limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

Errors use one JSON shape with timestamp, status, error, message, path, and optional field-level validation details.
