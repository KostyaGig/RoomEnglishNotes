package ru.kostya.roomnotes.listeners;

import ru.kostya.roomnotes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
