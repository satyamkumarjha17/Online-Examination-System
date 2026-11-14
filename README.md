# Online Examination System

A browser-based computer-based testing application built with Java 21. The project preserves the original three-subject question bank and examination rules, while replacing the Swing desktop interface with a responsive web interface.

> **Project type:** academic demonstration / practice assessment. It is not an enterprise examination platform and does not provide identity verification, proctoring, encrypted question-bank storage, or high-availability infrastructure.

## Team

Project team members (individual names intentionally omitted from this public package).

Chandigarh University · Computer Science & Engineering

## Design direction

The interface takes broad inspiration from professional assessment experiences such as SHL: restrained enterprise styling, clear candidate instructions, low-distraction question layouts, visible timing and consistent response-state signalling. This is an independent academic implementation. It does not use SHL logos, proprietary assets or copied product screens, and is not affiliated with SHL.

The assessment always opens in a separate browser tab after the candidate confirms the instructions. The portal remains in the original tab. The question palette uses five mutually exclusive visual states: Answered, Current, Seen/unanswered, Review and Not visited.

## Highlights

- Responsive student portal with student name, roll number and subject selection.
- Three subjects: Java, Aptitude and General Knowledge.
- 30 existing questions per subject; 15 are selected randomly for each attempt.
- 15-minute server-enforced examination timer and automatic submission after expiry.
- Question palette with five distinct states: answered, current, seen/unanswered, review and not visited.
- Save and next, previous, clear response and mark for review actions.
- One mark per correct answer; no negative marking; 40% passing threshold.
- Instant score, percentage, grade, pass status and answer-by-answer review.
- Administrator-only result table protected by an environment-configured PIN.
- CSV result persistence, configurable for a mounted persistent disk in deployment.
- Dockerfile for deployment on container-based hosting platforms.

## Technology

- Java 21
- Java built-in `HttpServer` (`jdk.httpserver` module)
- HTML5, CSS3 and vanilla JavaScript
- JSON API
- CSV file persistence
- Docker / Render-compatible container deployment

This version intentionally has no Maven or third-party Java dependencies. A JDK 21+ installation is sufficient to compile and run it. The built-in HTTP server keeps the project easy to reproduce in a clean lab environment. A production system with multiple application instances should replace the CSV store and in-memory sessions with a managed database and shared session storage.

## Run locally on Windows

1. Install JDK 21 or later.
2. Extract the ZIP file.
3. Open the extracted `Online-Examination-Web` folder in VS Code.
4. Open Terminal → New Terminal.
5. Run:

```powershell
java -version
javac -version
.\run.bat
```

6. Open <http://localhost:8080> in your browser.
7. Stop the server with `Ctrl+C` in the terminal.

The script compiles the Java source, creates `build/online-examination-web.jar`, and starts the server. It can be run again after changing the source.

## Run locally on macOS / Linux

```bash
java -version
javac -version
chmod +x run.sh
./run.sh
```

Then open <http://localhost:8080>.

## Administrator access

Select **Administrator** in the top navigation. The local default PIN is `admin123` for first-run convenience. Change it before sharing or deploying:

- Windows PowerShell: `$env:ADMIN_PIN="your-long-unique-pin"; .\run.bat`
- macOS / Linux: `ADMIN_PIN="your-long-unique-pin" ./run.sh`

For a hosted deployment, set `ADMIN_PIN` in the hosting provider's environment settings. Do not commit a real PIN or password to GitHub.

## Deployment on Render

This repository includes a Dockerfile and can be deployed as a Render Web Service.

1. Push this project to a GitHub repository.
2. In Render, choose **New → Web Service** and connect the repository.
3. Select **Docker** as the runtime.
4. Set the health check path to `/api/health` if the dashboard offers that setting.
5. Add environment variable `ADMIN_PIN` with a long, unique value.
6. Add a persistent disk mounted at `/var/data` if your Render plan supports it.
7. Add environment variable `DATA_FILE=/var/data/results.csv` so result records survive container restarts and redeployments.
8. Deploy. Render supplies the `PORT` environment variable; the application binds to `0.0.0.0` and reads that value automatically.

If persistent disks are not available on the selected plan, the application can still run, but local CSV records may be lost when the container is replaced. Do not treat ephemeral container storage as a permanent database.

## Project structure

```text
Online-Examination-Web/
├── src/main/java/com/exam/
│   ├── data/QuestionBank.java
│   ├── model/Question.java
│   └── web/ExamWebApplication.java
├── src/main/resources/public/
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── docs/
├── data/
├── Dockerfile
├── .dockerignore
├── .gitignore
├── run.bat
└── run.sh
```

## API overview

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Hosting health check |
| GET | `/api/config` | Subjects and exam settings |
| POST | `/api/exams` | Start a randomized exam |
| GET | `/api/exams/current` | Read current exam state |
| POST | `/api/exams/answer` | Save a selected option |
| POST | `/api/exams/visit` | Mark a question as visited |
| POST | `/api/exams/mark` | Toggle review marker |
| POST | `/api/exams/clear` | Clear a response |
| POST | `/api/exams/submit` | Submit and calculate result |
| POST | `/api/admin/login` | Authenticate administrator PIN |
| GET | `/api/admin/results` | Read submitted results (admin session required) |

## Examination rules

- 15 questions per attempt, selected from 30 questions in the chosen subject.
- 15 minutes per attempt.
- 1 mark per correct response.
- No negative marking.
- 40% is the pass threshold.
- Grade bands: A+ (90+), A (80+), B+ (70+), B (60+), C (50+), D (40+), F (below 40).

## Validation checklist

- [ ] Start the server and open the student portal.
- [ ] Verify empty student details are rejected.
- [ ] Start an exam for each of the three subjects.
- [ ] Verify 15 questions load and the timer counts down.
- [ ] Save, clear, navigate and mark questions for review.
- [ ] Submit with a mix of correct, incorrect and unanswered questions.
- [ ] Confirm result totals and answer review.
- [ ] Sign in as administrator and confirm the result appears.
- [ ] Restart the server and confirm CSV results reload.
- [ ] Verify `/api/health` returns `status: ok`.

The validation checklist is a test plan, not a claim that every item has been tested in every browser or hosting environment.

## Known limitations

- Student entry uses name and roll number, not verified university authentication.
- Admin access uses a shared PIN; use a strong secret and HTTPS when hosted.
- Sessions are held in server memory and are lost on process restart.
- CSV persistence is suitable for a small demonstration, not concurrent/high-volume production use.
- A single server instance is assumed. Multiple instances require shared session state and a database.
- No webcam proctoring, plagiarism detection, question authoring panel or password-based student accounts are included.

## License

No open-source license has been assigned. Ask the project team before reusing or redistributing the source code.
