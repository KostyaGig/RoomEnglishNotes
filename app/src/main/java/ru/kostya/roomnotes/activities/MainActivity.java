package ru.kostya.roomnotes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ru.kostya.roomnotes.R;
import ru.kostya.roomnotes.adapters.NoteAdapter;
import ru.kostya.roomnotes.database.NoteDatabase;
import ru.kostya.roomnotes.entities.Note;
import ru.kostya.roomnotes.listeners.NotesListener;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private int noteClickedPosition = -1;

    private RecyclerView noteRecView;
    private NoteAdapter adapter;
    private List<Note> noteList;

    private AlertDialog dialogAddUrl;

    private EditText edSearch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteRecView = findViewById(R.id.noteRecView);
        noteRecView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));

        noteList = new ArrayList<>();

        adapter = new NoteAdapter(MainActivity.this,noteList,this);


        ImageView addNoteImage = findViewById(R.id.imageAddNoteMain);
        addNoteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createNoteActivity = new Intent(MainActivity.this,CreateNoteActivity.class);
                startActivityForResult(createNoteActivity,REQUEST_CODE_ADD_NOTE);
            }
        });

        findViewById(R.id.addNoteImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createNoteActivity = new Intent(MainActivity.this,CreateNoteActivity.class);
                startActivityForResult(createNoteActivity,REQUEST_CODE_ADD_NOTE);
            }
        });

        findViewById(R.id.addImageImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                } else {
                    //Если дали разрешение
                    selectImage();
                }
            }
        });

        findViewById(R.id.addWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddUrlDialog();
            }
        });

        edSearch = findViewById(R.id.edSearch);
        edSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        adapter.cancelTimer();
                    }

                    @Override
                    public void afterTextChanged(Editable text) {
                        if (noteList.size() != 0){
                            adapter.searchNotes(text.toString());
                        }
                    }
                }
        );

        //Просто получаем notes для отображения
        getNotes(REQUEST_CODE_SHOW_NOTES,false);
    }

    //С помощью этого метода будем получать все наши заметки из нашей бд
    private void getNotes(final int requestCode,final Boolean isNoteDeleted){

        //2 ПОТОk НУЖЕН ДЛЯ получения всех Note из бд как бы чтобы не тормозить приложение
        //Второй поток,3 параметром указываем то,что мы хотим получать в методе onPostExecute
        class GetNotesTask extends AsyncTask<Void,Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                //Инициализируем нашу базу данных
                NoteDatabase noteDatabase = NoteDatabase.getDatabase(MainActivity.this);
                //Получаем все Note из нашей бд,присваиваем его нашей переменной allNotes и возвращаем из метода
                //А в методе onPostExecute(срабатывающий после долгих операций) мы можем получать данный список со всеми Note в бд
                //И там же ,во втором потоке устанавливать этот лист нашему recyclerView
                //С помощью AsyncTask мы можем обновлять View,поэтому не должны словить Exception
                List<Note> allNotes = noteDatabase.getNoteDao().getAllNotes();

                return allNotes;
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                //notes - и есть список со всеми нашими Note,которые сейчас в бд,именню это список мы возвращали из
                //метода doInBackground

                //Если requestcode равен отображать Notes
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    noteList.addAll(notes);
                    adapter.notifyDataSetChanged();
                    }
                //Если requestcode равен добавить Notes
                else if (requestCode == REQUEST_CODE_ADD_NOTE){
                    //ТОЕСТЬ после добавления новой Notes,добавляем в список новую запись под идексом 0,и самую первую в списке,и это иесть наша новая тольеко что добавленнаязапись
                        notes.add(0,notes.get(0));
                        adapter.notifyItemInserted(0);
                        noteRecView.smoothScrollToPosition(0);
                    }
                //Если requestcode равен обновить Notes
                else if (requestCode == REQUEST_CODE_UPDATE_NOTE){
                    //Удаляем
                        notes.remove(noteClickedPosition);
                        //добавляем
                        notes.add(noteClickedPosition,notes.get(noteClickedPosition));
//                        обновляем
                        adapter.notifyItemChanged(noteClickedPosition);

                        if (isNoteDeleted){
                            adapter.notifyItemRemoved(noteClickedPosition);
                        } else {
                            //добавляем
                            notes.add(noteClickedPosition,notes.get(noteClickedPosition));
//                        обновляем
                            adapter.notifyItemChanged(noteClickedPosition);
                        }
                    }
                noteRecView.setAdapter(adapter);

                }

            }
        //Запускаем наш поток
        new GetNotesTask().execute();
        }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK){
            //Получаем notes после добавления
            getNotes(REQUEST_CODE_ADD_NOTE,false);
            //Если это обновления записи,то получаем записи после обновления
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        } else if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
                Uri selectImageUri = data.getData();
                if (selectImageUri != null){
                    try {
                        String selectImagePath = getPathFomUri(selectImageUri);
                        Intent intent = new Intent(MainActivity.this,CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","image");
                        intent.putExtra("imagePath",selectImagePath);
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

    @Override
    public void onNoteClicked(Note note, int position) {
        //По нажатию на item мы будем запускать интент,при этом отправлять Note на которую нажали и еще boolean value
        noteClickedPosition = position;

        Intent updateNote = new Intent(MainActivity.this,CreateNoteActivity.class);
        updateNote.putExtra("isViewOrUpdate",true);
        updateNote.putExtra("note",note);
        startActivityForResult(updateNote,REQUEST_CODE_UPDATE_NOTE);
    }

    private void selectImage(){
        Intent selectImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (selectImageIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(selectImageIntent,REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0){
            //Если дали разрешение
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            } else {
                Toast.makeText(this, "Отсутствует разрешение на считывание данных из ExternalStroarge", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPathFomUri(Uri contentUri){
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if(cursor == null){
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    //Создаем диалоговое окно для Добавления url,и не только...
    private void showAddUrlDialog(){
        if (dialogAddUrl == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            //Раздуваем наш собственный add url layout
            View view = LayoutInflater.from(MainActivity.this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer),
                    false);

            //Устнавливаем view билдеру
            builder.setView(view);
            //Создаем
            dialogAddUrl = builder.create();

            if (dialogAddUrl.getWindow() != null){
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            //Находим editText
            final EditText edUrl = view.findViewById(R.id.edUrl);
            edUrl.requestFocus();

            //По нажатию на textView "Добавить"
            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (TextUtils.isEmpty(edUrl.getText().toString().trim())){

                        Toast.makeText(MainActivity.this, "Вставьте ссылку", Toast.LENGTH_SHORT).show();

                        //Проверка url на корректность
                    } else if (!Patterns.WEB_URL.matcher(edUrl.getText().toString().trim()).matches()){
                        //Если некорректный url
                        Toast.makeText(MainActivity.this, "Некорректный url", Toast.LENGTH_SHORT).show();
                    } else {
                        //Если все проходит успешно

                        Intent intent = new Intent(MainActivity.this,CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","URL");
                        intent.putExtra("URL",edUrl.getText().toString().trim());
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);

                        //Отключаем dialog
                        dialogAddUrl.dismiss();
                    }

                }
            });

            //По нажатию на textView "Отмена"
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddUrl.dismiss();
                }
            });

            //Отображаем dialog
            dialogAddUrl.show();
        }
    }


}