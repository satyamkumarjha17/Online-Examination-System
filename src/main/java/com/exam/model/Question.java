package com.exam.model;

import java.util.List;

/** One MCQ question of the examination. */
public class Question {
    private final String subject;
    private final String text;
    private final List<String> options;   // always 4
    private final int correctOption;      // 0-based index

    public Question(String subject, String text, List<String> options, int correctOption) {
        this.subject = subject;
        this.text = text;
        this.options = options;
        this.correctOption = correctOption;
    }

    public String getSubject() { return subject; }
    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public int getCorrectOption() { return correctOption; }

    public boolean isCorrect(int chosen) { return chosen == correctOption; }
}
