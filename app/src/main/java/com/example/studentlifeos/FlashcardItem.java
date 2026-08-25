package com.example.studentlifeos;

/**
 * Simple data holder for a flashcard. Matches the "flashcards" Firestore collection:
 * unitId, subjectId, studentId, front, back, source ("manual" | "generated"), createdAt.
 */
public class FlashcardItem {
    public String id;
    public String unitId;
    public String front;
    public String back;
    public String source; // "manual" or "generated"

    public FlashcardItem() {
    }

    public FlashcardItem(String id, String unitId, String front, String back, String source) {
        this.id = id;
        this.unitId = unitId;
        this.front = front;
        this.back = back;
        this.source = source;
    }
}
