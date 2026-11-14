# Project Synopsis: Online Examination System (Web Edition)

## Team

Project team · names intentionally omitted  
Chandigarh University · Computer Science & Engineering

## Problem statement

Paper-based objective examinations require manual distribution, checking and result preparation. This increases turnaround time and makes consistent time enforcement and immediate answer review difficult.

## Proposed solution

The web edition provides a browser-based examination flow. A student enters a name and roll number, selects a subject, reads the instructions and completes a timed multiple-choice assessment. The server selects a randomized set of questions, tracks responses and calculates the result after submission.

## Modules

1. Student entry and examination instructions.
2. Question bank for Java, Aptitude and General Knowledge.
3. Randomized 15-question assessment drawn from 30 questions per subject.
4. Server-enforced 15-minute timer and automatic submission.
5. Question navigation, response clearing and review marking.
6. Result calculation and answer-by-answer review.
7. Administrator view of submitted result records.

## Technology stack

- Java 21 and the JDK `HttpServer` module.
- HTML, CSS and vanilla JavaScript.
- JSON over HTTP for browser/server communication.
- CSV file for small-scale result persistence.
- Docker for containerized hosting.

## Interface design direction

The browser interface uses an independent enterprise-assessment visual direction informed by common patterns in professional assessment platforms, including SHL: clear candidate instructions, restrained colours, readable hierarchy and persistent assessment navigation. No third-party logos or proprietary screens are reproduced, and the project is not affiliated with SHL. The assessment launches in a new browser tab after the candidate confirms the instructions. The question palette distinguishes Answered, Current, Seen/unanswered, Review and Not visited states.

## Assessment rules

15 questions per attempt, 15 minutes, one mark per correct response, no negative marking and a 40% pass threshold. The grade bands are A+ (90+), A (80+), B+ (70+), B (60+), C (50+), D (40+) and F (below 40).

## Scope and limitations

This is an academic demonstration suitable for a small practice assessment. Student identity is not verified through a university login provider. Administrator access uses a shared PIN. Sessions are in memory, and CSV storage is not intended for high-volume concurrent production workloads. A larger deployment should use a managed relational database, proper user authentication, shared session storage, audit logging and operational monitoring.
