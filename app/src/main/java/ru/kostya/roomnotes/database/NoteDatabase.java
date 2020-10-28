package ru.kostya.roomnotes.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ru.kostya.roomnotes.dao.NoteDao;
import ru.kostya.roomnotes.entities.Note;

@Database(entities = Note.class,version = 1,exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static NoteDatabase noteDatabase;

    public static synchronized NoteDatabase getDatabase(Context context){
        if (noteDatabase == null){
            noteDatabase = Room.databaseBuilder(context, NoteDatabase.class,"Notes.db").build();
        }

        return noteDatabase;
    }

    public abstract NoteDao getNoteDao();
}
