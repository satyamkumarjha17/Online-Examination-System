# Architecture Notes

## Request flow

The browser serves a single-page interface from the Java HTTP server. The browser calls same-origin JSON endpoints. The server creates a session cookie, selects a randomized paper, owns the answer state and enforces the deadline. Correct answers are never included in the active-exam payload. They are returned only after submission in the review response.

## Main components

- `QuestionBank`: original subject question sets.
- `Question`: immutable question model.
- `ExamWebApplication`: HTTP routing, session state, exam rules, grading, result persistence and static asset delivery.
- `index.html`: semantic page structure and accessible labels.
- `styles.css`: responsive design system and screen layouts.
- `app.js`: browser state, navigation, timer display, API calls and result rendering.

## Data and deployment boundaries

- The exam deadline is stored server-side and checked on each exam API request. The browser timer is a display aid, not the authority.
- Result rows are appended to CSV. `DATA_FILE` can point to a mounted persistent volume.
- Student sessions and administrator authentication are held in memory. A process restart ends active sessions.
- For production scale, replace CSV with PostgreSQL, use a shared session store, add per-user authentication and role-based authorization, rate-limit login endpoints, add audit events and configure backups.
