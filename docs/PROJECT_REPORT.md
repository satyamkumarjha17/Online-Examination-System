# Online Examination System
## Web Edition · Project Report

**Project team:** Chandigarh University · Computer Science & Engineering  
**Department:** Computer Science & Engineering  
**Institution:** Chandigarh University  
**Implementation:** Java 21, browser frontend and Docker-ready HTTP service

---

## Abstract

The Online Examination System is a browser-based multiple-choice assessment application. It provides student entry, subject selection, randomized question papers, a timed examination interface, question navigation, automatic submission, result calculation and answer review. The web edition retains the original question-bank content for Java, Aptitude and General Knowledge while replacing the Swing desktop interface with a responsive browser interface.

The server selects 15 questions from the 30-question bank for the selected subject. Each attempt has a 15-minute time limit. The server stores answer state during the session and calculates the result on submission. Results are appended to a CSV file and can be viewed through a PIN-protected administrator screen.

## 1. Problem definition

Paper-based objective assessments require manual checking and result preparation. This delays feedback and makes strict, consistent time enforcement difficult. A small assessment tool should make it straightforward to run a timed practice test and provide immediate feedback.

## 2. Objectives

- Provide a responsive browser-based examination interface.
- Retain the three subject categories and existing question bank.
- Randomize question selection for each attempt.
- Enforce a fixed assessment duration.
- Support answer selection, navigation, clearing and review marking.
- Calculate marks and grade consistently.
- Present an answer-by-answer review after submission.
- Keep an administrator view of recorded attempts.

## 3. Scope

The project is designed as an academic demonstration and small practice-assessment application. It is not intended to replace a secure institutional examination platform. It does not implement verified university identity, remote proctoring, plagiarism detection, a question-authoring console or multi-instance distributed infrastructure.

## 4. Functional requirements

1. Student enters name and roll number.
2. Student selects Java, Aptitude or General Knowledge.
3. The server chooses 15 questions randomly from the selected 30-question bank.
4. The student has 15 minutes to complete the assessment.
5. The interface provides a question palette and review markers.
6. The student may clear a response or move between questions.
7. Submission calculates correct, incorrect and unanswered counts.
8. The result includes percentage, grade and pass status.
9. The student receives a complete answer review.
10. An administrator can view stored result records after PIN authentication.

## 5. Assessment rules

- 15 questions per attempt.
- 15 minutes per attempt.
- One mark for each correct answer.
- No negative marking.
- Passing threshold: 40%.
- Grade bands: A+ (90% and above), A (80–89.9%), B+ (70–79.9%), B (60–69.9%), C (50–59.9%), D (40–49.9%), F (below 40%).

## 6. Technology and architecture

The server uses Java 21 and the JDK `HttpServer` module. The browser uses HTML, CSS and vanilla JavaScript. The browser and server communicate through same-origin JSON endpoints. The server owns the answer state and exam deadline; the browser timer is a visual countdown. Correct answers are withheld from the active exam payload and included in the result only after submission.

The project avoids external Java dependencies so it can be built with a JDK alone. The Dockerfile packages the same application for a container host.

## 7. Data storage

Submitted results are appended to a CSV file. The path is configurable through `DATA_FILE`, allowing the host to mount persistent storage. The application loads the CSV records at startup. The CSV approach is intentionally limited to a small demonstration. A larger deployment should use PostgreSQL or another managed relational database with migrations, backups and transactional writes.

## 8. Security considerations

The application uses an HTTP-only, SameSite session cookie. Student text is rendered as text rather than trusted HTML. The administrator endpoint requires a PIN stored in the `ADMIN_PIN` environment variable. The local default PIN exists only to simplify first-run testing and must be changed before deployment.

This is not a complete security model. Production use requires individual student accounts, proper password hashing or institutional SSO, role-based access control, rate limiting, audit logging, stronger session management, CSRF review, privacy controls and security testing.

## 9. Verification

The source was compiled with Java 21 in the build environment. The health endpoint and static asset routes returned successful responses. Integration checks exercised exam creation for all three subjects, 15-question payloads, answer submission, review marking, final submission, administrator authentication, result listing and CSV reload after server restart.

These checks validate the tested API flows. They do not constitute a full browser compatibility, accessibility, load, penetration or production-hosting certification. Run the included test plan on the target Windows machine and hosted environment before a formal demonstration.

## 10. Limitations and future work

- Replace CSV storage with PostgreSQL.
- Replace in-memory sessions with a shared session store.
- Add individual student and administrator accounts.
- Add a question management interface and configurable exams.
- Add audit logging, backups and monitoring.
- Add accessibility and cross-browser testing.
- Add load testing and security review before institutional use.

## Conclusion

The web edition moves the original desktop examination workflow into a browser-accessible application while retaining its core question-bank and scoring rules. It is suitable as a working academic prototype and a foundation for further development. Its documented storage and authentication limitations should be addressed before any high-stakes or large-scale use.
