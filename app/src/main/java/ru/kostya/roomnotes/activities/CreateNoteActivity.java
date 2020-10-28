package ru.kostya.roomnotes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.kostya.roomnotes.R;
import ru.kostya.roomnotes.database.NoteDatabase;
import ru.kostya.roomnotes.entities.Note;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText edNoteTitle,edNoteSubTitle,edNoteText;
    private ImageView saveImage,backImage,imageNote;
    private TextView textDateTime,textWebUrl;

    private LinearLayout layoutWebUrl;

    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    public static final int REQUEST_CODE_SELECT_IMAGE = 2;

    //Это есть наша палочка рядом с editText subtitle,чекни
    private View viewSubTitleIndicator;
    private String selectNoteColor;

    //Когда мы добавляем запись и в записи есть фото,то при сохранении Note в бд мы будем сохранять путь картинки
    //После в recycler будем отображать данную картинку по пути
    private String selectImagePath;

    private AlertDialog dialogAddUrl,dialogDeleteNote;

    //Уже доступная запись
    private Note alreadyAvailableNotes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        init();


        //По нажатию на imageView "сохранить заметку"
        saveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Вызываем метод,который возвращает нас на предыдущее активити
                onBackPressed();
            }
        });


        //Устанвливаем в методе onCreate сразу же дату нашему textView,а при вставке Note мы устанавливаем с пмоощью note.setDateTime(textDateTime.getText().toString())
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault());
        textDateTime.setText(simpleDateFormat.format(new Date()));

        //Принимаем intent,при нажатии на recview
        if (getIntent().getBooleanExtra("isViewOrUpdate",false)){
            //Присваиваем нашей переменной notes Note,на которую мы нажимаем в recyclerview
            alreadyAvailableNotes = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNotes();
        }

        findViewById(R.id.imageRemoveUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textWebUrl.setText(null);
                layoutWebUrl.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectImagePath = "";
            }
        });




        //MainActivity 192стр.кода
        if (getIntent().getBooleanExtra("isFromQuickActions",false)){
            String type = getIntent().getStringExtra("quickActionType");

            if (type != null) {
                if (type.equals("image")) {
                    String imagePath = getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                    imageNote.setVisibility(View.VISIBLE);

                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                } else if (type.equals("URL")){
                    String url = getIntent().getStringExtra("URL");
                    textWebUrl.setText(url);
                    layoutWebUrl.setVisibility(View.VISIBLE);
                }
            }
        }

        initMiscellaneous();
        setSubTitleIndicator();
    }

    private void init() {
        edNoteTitle = findViewById(R.id.edNoteTitle);
        edNoteSubTitle = findViewById(R.id.edNoteSubTitle);
        edNoteText = findViewById(R.id.edNoteText);

        saveImage = findViewById(R.id.imageSave);
        backImage = findViewById(R.id.backImage);
        imageNote = findViewById(R.id.imageNote);

        textDateTime = findViewById(R.id.textDateTime);
        textWebUrl = findViewById(R.id.textWebUrl);

        layoutWebUrl = findViewById(R.id.layoutWebUrl);

        viewSubTitleIndicator = findViewById(R.id.viewSubTitleIndicator);

        //Выбранны цвет,устанавливаем по умолчанию #333333
        selectNoteColor = "#333333";

        //default initialize
        selectImagePath = "";
    }

    private void setViewOrUpdateNotes(){
        //Теперь устанавливаем нашим View те значение,которые были в нажатом нами Note из recyclerview

        edNoteTitle.setText(alreadyAvailableNotes.getTitle());
        edNoteSubTitle.setText(alreadyAvailableNotes.getSubTitle());
        edNoteText.setText(alreadyAvailableNotes.getNoteText());
        textDateTime.setText(alreadyAvailableNotes.getDateTime());

        //Устнавливаем нашей переменной с цветом тот,который находился в нажатом нами Notes
        selectNoteColor = alreadyAvailableNotes.getColor();

        //Если у нажатой нами Notes есть картинка
        if (alreadyAvailableNotes.getImagePath() != null && !alreadyAvailableNotes.getImagePath().equals("")) {
            //Если путь КАРТИНКИ НАЖАТОЙ НАМИ Notes не пустой,то
            //Устнавливаем нашей переменной с путем,по которому хранится картинка ту,которая находилась в нажатом нами Notes
            selectImagePath = alreadyAvailableNotes.getImagePath();
            //Устанавливаем нашей imageView ту картинку,которая находлась в нажатой нами ранне в recviewnotes
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNotes.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);

            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
        }

        //Если ссылка наэатой нами Notes в recview не пустая,то устанавливаем ее нашему textWebUrl
        if (alreadyAvailableNotes.getWebLink() != null && !alreadyAvailableNotes.getWebLink().trim().isEmpty()){
                textWebUrl.setText(alreadyAvailableNotes.getWebLink());
                layoutWebUrl.setVisibility(View.VISIBLE);
        }

    }

    private void saveNote(){

        //Если поля не пустые,то...
        if (!TextUtils.isEmpty(edNoteTitle.getText().toString().trim()) && !TextUtils.isEmpty(edNoteSubTitle.getText().toString().trim()) && !TextUtils.isEmpty(edNoteText.getText().toString().trim())){


            final Note note = new Note();
            note.setTitle(edNoteTitle.getText().toString().trim());
            note.setSubTitle(edNoteSubTitle.getText().toString().trim());
            note.setNoteText(edNoteText.getText().toString().trim());
            note.setDateTime(textDateTime.getText().toString());
            note.setColor(selectNoteColor);
            note.setImagePath(selectImagePath);

            //Если layout с url видим, а он может быть видим только в том случае,что когда мы в dialog кликаем на "Добавить"
            // наша ссылка которую мы вставляем проходит все проверки,чекай метод showdialog view.fv(R.id.addText)
            //И если наша ссылка не пустая и проходит проверку на корректность,то мы отображаеем layout с url
            if (layoutWebUrl.getVisibility() == View.VISIBLE){

                note.setWebLink(textWebUrl.getText().toString());
            }

            //Если нажатая нами заметка не пустая,то ее id устанавливаем той, которая теперь открыта,тоесть получеатся это и есть наша Note
            //С помощью onConflict = REPLACE В методе insert нашего NoteDao все пройдет хорошо,
            // тоесть проверится id нашей note и если она уже существует,то ее id заменится той на которую мы нажали

            if (alreadyAvailableNotes != null){
                note.setId(alreadyAvailableNotes.getId());
            }
            //Создаем внутренний класс,наследуемый от AsyncTask
            //Что за аннртауич - не знаю
            @SuppressLint("StaticFieldLeak")
            class SaveNoteTask extends AsyncTask<Void,Void,Void>{

                @Override
                protected Void doInBackground(Void... voids) {
                    //Метод,который выполняет длительные операции

                    //Инициализируем нашу бд
                    NoteDatabase noteDatabase = NoteDatabase.getDatabase(CreateNoteActivity.this);

                    //Вставляем нашу Note в бд
                    noteDatabase.getNoteDao().insertNote(note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);

                    //Выполняется после длительной работы
                    //Отправляем юзера на главную активность приложения
                    Intent mainActivity = new Intent(CreateNoteActivity.this,MainActivity.class);
                    //Присволиил результат,чтобы в MainActivity в onActivityResult отлавливать его, и вызывать метод getNotes
                    //Это нужно чтобы при добавлении новой записи мы переходили в MainActivty и новая запись сразу же отображалась в recyclerview
                    //А так бы новая запись отображалась при перезаходе приложения с помощью метода oncreate mainactivity,в котором мы и вызываем метод getNotes
                    setResult(RESULT_OK,mainActivity);
                    startActivity(mainActivity);
                    finish();
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

            }

            //После описания класса создаем его обЪект и запускаем наш поток,вставляя запись в бд
            new SaveNoteTask().execute();

        } else {
            Toast.makeText(this, "Заполните пустые поля!", Toast.LENGTH_SHORT).show();
        }

    }

    //Связано с цветами
    //Miscellaneous в пер. с англ = Разнообразный (типо разноцветный)
    private void initMiscellaneous(){

        final LinearLayout miscellaneousLayout = findViewById(R.id.layoutMicellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(miscellaneousLayout);
        //Код выше нам нужен только для того чтобы получить доступ к View
        miscellaneousLayout.findViewById(R.id.textMiscellaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //По нажатию на текствью с текстом Miscellaneous будет появляться прикольное bottomsheet,проще увидеть
                if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        //Находим все цвета


        final ImageView imageColor1 = miscellaneousLayout.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = miscellaneousLayout.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = miscellaneousLayout.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = miscellaneousLayout.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = miscellaneousLayout.findViewById(R.id.imageColor5);

        //По нажатию на 1 цвет
        miscellaneousLayout.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectNoteColor = "#333333";
                imageColor1.setImageResource(R.drawable.ic_baseline_done_24);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);

                //Вызываем метод обновления цвета индикатора
                setSubTitleIndicator();
            }
        });

        //По нажатию на 2 цвет и так далее
        miscellaneousLayout.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectNoteColor = "#FDBE3B";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_baseline_done_24);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);

                //Вызываем метод обновления цвета индикатора
                setSubTitleIndicator();
            }
        });

        miscellaneousLayout.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectNoteColor = "#FF4842";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_baseline_done_24);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);

                //Вызываем метод обновления цвета индикатора
                setSubTitleIndicator();
            }
        });

        miscellaneousLayout.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectNoteColor = "#3A52FC";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_baseline_done_24);
                imageColor5.setImageResource(0);

                //Вызываем метод обновления цвета индикатора
                setSubTitleIndicator();
            }
        });

        miscellaneousLayout.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectNoteColor = "#000000";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_baseline_done_24);

                //Вызываем метод обновления цвета индикатора
                setSubTitleIndicator();
            }
        });

        //Если нажатая нами заметка в recview не null и цвет не null
        if (alreadyAvailableNotes != null && alreadyAvailableNotes.getColor() != null &&!alreadyAvailableNotes.getColor().trim().isEmpty()){
            //В зависсимости от того,какой цвет хранится в нашей нажетой Notes устанавливаем нашему viewSubTitleIndicator
            switch (alreadyAvailableNotes.getColor()){
                case "#333333":
                    //Если это цвет по дефолту (#333333) делаем нажатой наш первый viewcolor
                    miscellaneousLayout.findViewById(R.id.viewColor1).performClick();
                    break;
                case "#FDBE3B":
                    miscellaneousLayout.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    miscellaneousLayout.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52FC":
                    miscellaneousLayout.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    miscellaneousLayout.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        //По нажатию на layout с добавить фотографию
        miscellaneousLayout.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Чекаем разрешения на считывание данных из External storage
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                //Если нам не арзершили доступ,то получаем его (269 стр. кода)
                if (ContextCompat.checkSelfPermission(CreateNoteActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(CreateNoteActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                } else {
                    //Если дали разрешение
                    selectImage();
                }
            }
        });

        //По нажатию на layout "Добавить ссылку"
        miscellaneousLayout.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddUrlDialog();
            }
        });

        //Если по нажатию на заметку она не равна нул,то отображаем наш layout с возможностью удалить заметку
        if (alreadyAvailableNotes != null){
            miscellaneousLayout.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            miscellaneousLayout.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    //Показываем диалогове окно
                    showDialogDeleteNote();
                }
            });

        }
    }

    private void showDialogDeleteNote(){

        if (dialogDeleteNote == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            View view = LayoutInflater.from(CreateNoteActivity.this)
                    .inflate(R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer),
                    false);

            builder.setView(view);

            dialogDeleteNote = builder.create();

            if (dialogDeleteNote.getWindow() != null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final TextView deleteNoteTextView = view.findViewById(R.id.deleteNoteText);
            final TextView cancelDeleteNoteTextView = view.findViewById(R.id.cancelDeleteNoteText);

            //По нажатию на текствью "Удалить"
            deleteNoteTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void,Void,Void>{

                        @Override
                        protected Void doInBackground(Void... voids) {
                            //initialize noteDatabase
                            NoteDatabase database = NoteDatabase.getDatabase(CreateNoteActivity.this);
                            //Удаляем нажатую нами запись
                            database.getNoteDao().deleteNote(alreadyAvailableNotes);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            //Примем этот интент в onactivityresult с public static final int REQUEST_CODE_UPDATE_NOTE = 2; вот таким request кодом
                            Intent mainIntent = new Intent();
                            mainIntent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                    //Запускаем наш поток
                    new DeleteNoteTask().execute();

                }
            });

            //По нажатию на текствью "Отмена"
            cancelDeleteNoteTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogDeleteNote.dismiss();
                }
            });
        }
        dialogDeleteNote.show();
    }

    private void setSubTitleIndicator(){
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubTitleIndicator.getBackground();
        //Устанавливаем цвет,из переменной selectNoteColor
        gradientDrawable.setColor(Color.parseColor(selectNoteColor));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && data != null && data.getData() != null){
            Uri selectImageUri = data.getData();

            if (selectImageUri != null){

                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    imageNote.setImageBitmap(bitmap);
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                    //Наш метод getPathFromUri
                    selectImagePath = getPathFomUri(selectImageUri);
                } catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

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
            View view = LayoutInflater.from(CreateNoteActivity.this).inflate(
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

                        Toast.makeText(CreateNoteActivity.this, "Вставьте ссылку", Toast.LENGTH_SHORT).show();

                        //Проверка url на корректность
                    } else if (!Patterns.WEB_URL.matcher(edUrl.getText().toString().trim()).matches()){
                        //Если некорректный url
                        Toast.makeText(CreateNoteActivity.this, "Некорректный url", Toast.LENGTH_SHORT).show();
                    } else {
                        //Если все проходит успешно

                    // Устанавливаем нашему текствью url,
                    // именно с него мы будем получть текст и отправлять в note.setWebLink(textWebUrl.getText().toString() - вот таким образом
                    textWebUrl.setText(edUrl.getText().toString().trim());
                    //Показываем layout
                    layoutWebUrl.setVisibility(View.VISIBLE);

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