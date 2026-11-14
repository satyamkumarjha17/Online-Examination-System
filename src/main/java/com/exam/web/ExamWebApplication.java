package com.exam.web;

import com.exam.data.QuestionBank;
import com.exam.model.Question;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight, dependency-free Java web server for the Online Examination System. */
public final class ExamWebApplication {
    private static final int QUESTION_COUNT = 15;
    private static final int EXAM_SECONDS = 15 * 60;
    private static final Path DATA_FILE = Path.of(System.getenv().getOrDefault("DATA_FILE", "data/results.csv"));
    private static final Path PUBLIC_DIR = Path.of(System.getenv().getOrDefault("PUBLIC_DIR", "src/main/resources/public"));
    private static final Map<String, UserSession> SESSIONS = new ConcurrentHashMap<>();
    private static final List<ResultRecord> RESULTS = new ArrayList<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ADMIN_PIN = System.getenv().getOrDefault("ADMIN_PIN", "admin123");

    private ExamWebApplication() { }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Files.createDirectories(DATA_FILE.getParent());
        if (!Files.exists(DATA_FILE)) Files.writeString(DATA_FILE, "id,submitted_at,student_name,roll_number,subject,correct,total,attempted,percentage,grade,seconds_used,passed" + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        loadResults();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(Math.max(8, Runtime.getRuntime().availableProcessors() * 2)));
        server.createContext("/api/", ExamWebApplication::handleApi);
        server.createContext("/", ExamWebApplication::handleStatic);
        server.start();
        System.out.println("Online Examination System is running on port " + port);
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            UserSession session = sessionFor(ex);
            if ("GET".equals(method) && "/api/health".equals(path)) {
                json(ex, 200, "{\"status\":\"ok\",\"service\":\"online-examination-web\"}"); return;
            }
            if ("GET".equals(method) && "/api/config".equals(path)) {
                json(ex, 200, "{\"subjects\":[\"Java\",\"Aptitude\",\"General Knowledge\"],\"questionCount\":" + QUESTION_COUNT + ",\"durationSeconds\":" + EXAM_SECONDS + "}"); return;
            }
            if ("POST".equals(method) && "/api/exams".equals(path)) {
                Map<String,String> body = readFields(ex);
                String name = clean(body.get("name"), 60);
                String roll = clean(body.get("rollNumber"), 30);
                String subject = clean(body.get("subject"), 40);
                if (name.isBlank() || roll.isBlank()) { error(ex, 400, "Enter your name and roll number."); return; }
                if (!QuestionBank.getSubjects().contains(subject)) { error(ex, 400, "Choose a valid subject."); return; }
                List<Question> bank = new ArrayList<>(QuestionBank.getQuestions(subject));
                Collections.shuffle(bank, RANDOM);
                Exam exam = new Exam(name, roll, subject, new ArrayList<>(bank.subList(0, Math.min(QUESTION_COUNT, bank.size()))));
                session.exam = exam;
                json(ex, 201, examPayload(exam, false)); return;
            }
            if ("POST".equals(method) && "/api/admin/login".equals(path)) {
                String pin = readFields(ex).getOrDefault("pin", "");
                if (!constantTimeEquals(pin, ADMIN_PIN)) { error(ex, 401, "The administrator PIN is incorrect."); return; }
                session.admin = true;
                json(ex, 200, "{\"authenticated\":true}"); return;
            }
            if ("GET".equals(method) && "/api/admin/results".equals(path)) {
                if (!session.admin) { error(ex, 401, "Administrator sign-in is required."); return; }
                json(ex, 200, resultsJson()); return;
            }
            // Clear only the current browser session's active/completed attempt.
            // Historical submitted results remain in the administrator's CSV records.
            if ("POST".equals(method) && "/api/session/reset".equals(path)) {
                session.exam = null;
                json(ex, 200, "{\"reset\":true}");
                return;
            }
            if (session.exam == null) { error(ex, 404, "No active examination. Start a new exam first."); return; }
            Exam exam = session.exam;
            if (exam.result != null) { json(ex, 200, exam.result); return; }
            if (System.currentTimeMillis() >= exam.endsAt) {
                finish(exam, true);
                json(ex, 200, exam.result); return;
            }
            if ("GET".equals(method) && "/api/exams/current".equals(path)) { json(ex, 200, examPayload(exam, false)); return; }
            if ("POST".equals(method) && "/api/exams/answer".equals(path)) {
                Map<String,String> body = readFields(ex);
                int index = intField(body, "index", -1), option = intField(body, "option", -1);
                if (!validIndex(exam, index) || option < 0 || option > 3) { error(ex, 400, "Invalid question or answer option."); return; }
                exam.visited[index] = true; exam.answers[index] = option;
                json(ex, 200, stateJson(exam)); return;
            }
            if ("POST".equals(method) && "/api/exams/visit".equals(path)) {
                int index = intField(readFields(ex), "index", -1);
                if (!validIndex(exam, index)) { error(ex, 400, "Invalid question number."); return; }
                exam.visited[index] = true; json(ex, 200, stateJson(exam)); return;
            }
            if ("POST".equals(method) && "/api/exams/mark".equals(path)) {
                Map<String,String> body = readFields(ex);
                int index = intField(body, "index", -1);
                if (!validIndex(exam, index)) { error(ex, 400, "Invalid question number."); return; }
                exam.visited[index] = true; exam.marked[index] = Boolean.parseBoolean(body.getOrDefault("marked", "false"));
                json(ex, 200, stateJson(exam)); return;
            }
            if ("POST".equals(method) && "/api/exams/clear".equals(path)) {
                int index = intField(readFields(ex), "index", -1);
                if (!validIndex(exam, index)) { error(ex, 400, "Invalid question number."); return; }
                exam.answers[index] = -1; exam.visited[index] = true;
                json(ex, 200, stateJson(exam)); return;
            }
            if ("POST".equals(method) && "/api/exams/submit".equals(path)) {
                finish(exam, false); json(ex, 200, exam.result); return;
            }
            error(ex, 404, "API endpoint not found.");
        } catch (Exception e) {
            e.printStackTrace(); error(ex, 500, "Something went wrong. Please retry.");
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..") || path.contains("\\")) { send(ex, 400, "text/plain; charset=utf-8", "Bad request"); return; }
        Path file = PUBLIC_DIR.resolve(path.substring(1)).normalize();
        if (!file.startsWith(PUBLIC_DIR)) { send(ex, 400, "text/plain; charset=utf-8", "Bad request"); return; }
        String type = switch (extension(file)) {
            case "html" -> "text/html; charset=utf-8"; case "css" -> "text/css; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8"; case "svg" -> "image/svg+xml";
            case "png" -> "image/png"; default -> "application/octet-stream";
        };
        byte[] bytes;
        if (Files.isRegularFile(file)) bytes = Files.readAllBytes(file);
        else {
            try (InputStream resource = ExamWebApplication.class.getResourceAsStream("/public/" + path.substring(1))) {
                if (resource == null) { send(ex, 404, "text/plain; charset=utf-8", "Not found"); return; }
                bytes = resource.readAllBytes();
            }
        }
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        ex.getResponseHeaders().set("X-Frame-Options", "DENY");
        ex.sendResponseHeaders(200, bytes.length); ex.getResponseBody().write(bytes); ex.close();
    }

    private static void finish(Exam exam, boolean timedOut) throws IOException {
        if (exam.result != null) return;
        int attempted = 0, correct = 0;
        List<String> review = new ArrayList<>();
        for (int i = 0; i < exam.questions.size(); i++) {
            Question q = exam.questions.get(i); int answer = exam.answers[i];
            if (answer >= 0) attempted++;
            if (q.isCorrect(answer)) correct++;
            String status = answer < 0 ? "Skipped" : q.isCorrect(answer) ? "Correct" : "Incorrect";
            review.add("{\"number\":" + (i + 1) + ",\"question\":" + quote(q.getText()) + ",\"options\":" + stringArray(q.getOptions()) +
                    ",\"selected\":" + answer + ",\"correct\":" + q.getCorrectOption() + ",\"status\":" + quote(status) + "}");
        }
        long now = System.currentTimeMillis();
        int secondsUsed = (int)Math.max(0, Math.min(EXAM_SECONDS, (now - exam.startedAt) / 1000));
        double percent = exam.questions.isEmpty() ? 0 : correct * 100.0 / exam.questions.size();
        String grade = grade(percent); boolean passed = percent >= 40;
        String submittedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(now).atOffset(ZoneOffset.UTC));
        String id = UUID.randomUUID().toString();
        exam.result = "{\"id\":" + quote(id) + ",\"studentName\":" + quote(exam.name) + ",\"rollNumber\":" + quote(exam.roll) +
                ",\"subject\":" + quote(exam.subject) + ",\"total\":" + exam.questions.size() + ",\"attempted\":" + attempted +
                ",\"correctCount\":" + correct + ",\"wrong\":" + (attempted - correct) + ",\"skipped\":" + (exam.questions.size() - attempted) +
                ",\"percentage\":" + String.format(java.util.Locale.US, "%.1f", percent) + ",\"grade\":" + quote(grade) +
                ",\"passed\":" + passed + ",\"timedOut\":" + timedOut + ",\"secondsUsed\":" + secondsUsed +
                ",\"submittedAt\":" + quote(submittedAt) + ",\"review\":[" + String.join(",", review) + "]}";
        ResultRecord record = new ResultRecord(id, submittedAt, exam.name, exam.roll, exam.subject, correct, exam.questions.size(), attempted, percent, grade, secondsUsed, passed);
        synchronized (RESULTS) { RESULTS.add(0, record); }
        String csv = csv(record) + System.lineSeparator();
        Files.writeString(DATA_FILE, csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String examPayload(Exam e, boolean includeAnswers) {
        List<String> qs = new ArrayList<>();
        for (int i = 0; i < e.questions.size(); i++) {
            Question q = e.questions.get(i);
            qs.add("{\"number\":" + (i + 1) + ",\"text\":" + quote(q.getText()) + ",\"options\":" + stringArray(q.getOptions()) + "}");
        }
        return "{\"studentName\":" + quote(e.name) + ",\"rollNumber\":" + quote(e.roll) + ",\"subject\":" + quote(e.subject) +
                ",\"durationSeconds\":" + EXAM_SECONDS + ",\"endsAt\":" + e.endsAt + ",\"questions\":[" + String.join(",", qs) + "],\"state\":" + stateJson(e) + "}";
    }
    private static String stateJson(Exam e) {
        List<String> answers = new ArrayList<>(), visited = new ArrayList<>(), marked = new ArrayList<>();
        for (int i = 0; i < e.answers.length; i++) { answers.add(String.valueOf(e.answers[i])); visited.add(String.valueOf(e.visited[i])); marked.add(String.valueOf(e.marked[i])); }
        return "{\"answers\":[" + String.join(",", answers) + "],\"visited\":[" + String.join(",", visited) + "],\"marked\":[" + String.join(",", marked) + "]}";
    }
    private static String resultsJson() {
        List<String> rows = new ArrayList<>();
        synchronized (RESULTS) { for (ResultRecord r : RESULTS) rows.add("{\"id\":" + quote(r.id) + ",\"submittedAt\":" + quote(r.submittedAt) + ",\"studentName\":" + quote(r.name) + ",\"rollNumber\":" + quote(r.roll) + ",\"subject\":" + quote(r.subject) + ",\"correct\":" + r.correct + ",\"total\":" + r.total + ",\"attempted\":" + r.attempted + ",\"percentage\":" + String.format(java.util.Locale.US,"%.1f",r.percentage) + ",\"grade\":" + quote(r.grade) + ",\"passed\":" + r.passed + "}"); }
        return "{\"results\":[" + String.join(",", rows) + "]}";
    }
    private static void loadResults() throws IOException {
        if (!Files.exists(DATA_FILE)) return;
        List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            List<String> f = parseCsv(lines.get(i)); if (f.size() < 12) continue;
            try { RESULTS.add(new ResultRecord(f.get(0),f.get(1),f.get(2),f.get(3),f.get(4),Integer.parseInt(f.get(5)),Integer.parseInt(f.get(6)),Integer.parseInt(f.get(7)),Double.parseDouble(f.get(8)),f.get(9),Integer.parseInt(f.get(10)),Boolean.parseBoolean(f.get(11)))); } catch (RuntimeException ignored) { }
        }
        Collections.reverse(RESULTS);
    }
    private static List<String> parseCsv(String line) { List<String> out=new ArrayList<>(); StringBuilder b=new StringBuilder(); boolean q=false; for(int i=0;i<line.length();i++){char c=line.charAt(i); if(c=='"' && q && i+1<line.length() && line.charAt(i+1)=='"'){b.append('"');i++;} else if(c=='"') q=!q; else if(c==','&&!q){out.add(b.toString());b.setLength(0);} else b.append(c);} out.add(b.toString()); return out; }
    private static String csv(ResultRecord r) { return String.join(",", csvCell(r.id),csvCell(r.submittedAt),csvCell(r.name),csvCell(r.roll),csvCell(r.subject),String.valueOf(r.correct),String.valueOf(r.total),String.valueOf(r.attempted),String.format(java.util.Locale.US,"%.1f",r.percentage),csvCell(r.grade),String.valueOf(r.secondsUsed),String.valueOf(r.passed)); }
    private static String csvCell(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
    private static UserSession sessionFor(HttpExchange ex) {
        String token = null; String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) for (String part : cookie.split(";")) { String p=part.trim(); if(p.startsWith("EXAM_SESSION=")) token=p.substring("EXAM_SESSION=".length()); }
        if (token != null && SESSIONS.containsKey(token)) return SESSIONS.get(token);
        token = UUID.randomUUID().toString(); UserSession s = new UserSession(); SESSIONS.put(token,s);
        ex.getResponseHeaders().add("Set-Cookie", "EXAM_SESSION="+token+"; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400"); return s;
    }
    private static Map<String,String> readFields(HttpExchange ex) throws IOException {
        String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); Map<String,String> map=new ConcurrentHashMap<>();
        Matcher m=Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|(-?\\d+)|(true|false|null))").matcher(raw);
        while(m.find()){String v=m.group(2)!=null?unescape(m.group(2)):m.group(3)!=null?m.group(3):m.group(4);map.put(m.group(1),v==null?"":v);} return map;
    }
    private static String unescape(String s) { return s.replace("\\\"","\"").replace("\\n"," ").replace("\\\\","\\"); }
    private static String clean(String s,int max) { if(s==null)return ""; String v=s.trim().replaceAll("[\\p{Cntrl}]", " "); return v.substring(0,Math.min(max,v.length())); }
    private static int intField(Map<String,String> m,String k,int fallback){try{return Integer.parseInt(m.getOrDefault(k,""));}catch(Exception e){return fallback;}}
    private static boolean validIndex(Exam e,int i){return i>=0&&i<e.questions.size();}
    private static String grade(double p){if(p>=90)return "A+";if(p>=80)return "A";if(p>=70)return "B+";if(p>=60)return "B";if(p>=50)return "C";if(p>=40)return "D";return "F";}
    private static boolean constantTimeEquals(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    private static String extension(Path p){String n=p.getFileName().toString();int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1).toLowerCase();}
    private static String stringArray(List<String> values){List<String> a=new ArrayList<>();for(String s:values)a.add(quote(s));return "["+String.join(",",a)+"]";}
    private static String quote(String s){if(s==null)return "null";return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
    private static void error(HttpExchange ex,int status,String message)throws IOException{json(ex,status,"{\"error\":"+quote(message)+"}");}
    private static void json(HttpExchange ex,int status,String body)throws IOException{send(ex,status,"application/json; charset=utf-8",body);}
    private static void send(HttpExchange ex,int status,String type,String body)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);Headers h=ex.getResponseHeaders();h.set("Content-Type",type);h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");h.set("Referrer-Policy","strict-origin-when-cross-origin");h.set("X-Frame-Options","DENY");ex.sendResponseHeaders(status,bytes.length);ex.getResponseBody().write(bytes);ex.close();}

    private static final class UserSession { Exam exam; boolean admin; }
    private static final class Exam {
        final String name,roll,subject; final List<Question> questions; final int[] answers; final boolean[] visited,marked; final long startedAt,endsAt; String result;
        Exam(String name,String roll,String subject,List<Question> questions){this.name=name;this.roll=roll;this.subject=subject;this.questions=questions;answers=new int[questions.size()];java.util.Arrays.fill(answers,-1);visited=new boolean[questions.size()];marked=new boolean[questions.size()];startedAt=System.currentTimeMillis();endsAt=startedAt+EXAM_SECONDS*1000L;visited[0]=true;}
    }
    private record ResultRecord(String id,String submittedAt,String name,String roll,String subject,int correct,int total,int attempted,double percentage,String grade,int secondsUsed,boolean passed) { }
}
