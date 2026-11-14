# Test Plan

| ID | Scenario | Expected result |
|---|---|---|
| T01 | Open `/` | Student portal loads |
| T02 | Open `/api/health` | JSON status is `ok` |
| T03 | Submit blank name or roll number | Request is rejected with a validation message |
| T04 | Select each supported subject | A 15-question paper for that subject starts |
| T05 | Select an answer | Selected option is retained in the current session |
| T06 | Clear an answer | Selected option is removed |
| T07 | Mark a question for review | Palette shows the review state |
| T08 | Navigate between questions | Current question and palette state update |
| T09 | Submit early | Score and review are returned |
| T10 | Reach the time limit | Server submits the attempt automatically on the next request |
| T11 | Open admin results without PIN | Access is rejected |
| T12 | Sign in with configured admin PIN | Submitted results are displayed |
| T13 | Restart server with the same data file | Previously written result rows are loaded |

This plan defines expected behavior. Run the checks in the target environment before presenting or publishing a release.
