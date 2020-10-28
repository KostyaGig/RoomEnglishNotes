package ru.kostya.roomnotes.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import ru.kostya.roomnotes.R;
import ru.kostya.roomnotes.entities.Note;
import ru.kostya.roomnotes.listeners.NotesListener;


public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context mContext;
    private List<Note> notes;
    private NotesListener listener;

    private Timer timer;
    private List<Note> noteSource;

    public NoteAdapter(Context mContext, List<Note> notes,NotesListener listener) {
        this.mContext = mContext;
        this.notes = notes;
        this.listener = listener;
        noteSource = notes;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }


    public class NoteViewHolder extends RecyclerView.ViewHolder{

        public TextView title,subTitle,textDateTime;

        //Это и есть наш layout(itemview) ему мы будем устанавливать background и тот цвет,который устнаовил пользователь при созлании заметки
        public LinearLayout layoutNote;

        public RoundedImageView roundedImageView;
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            subTitle = itemView.findViewById(R.id.subTitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);

            layoutNote = itemView.findViewById(R.id.layoutNote);

            roundedImageView = itemView.findViewById(R.id.imageNote);
        }

        void bind(Note note){
            title.setText(note.getNoteText());
            subTitle.setText(note.getSubTitle());
            textDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            //Устанавливаем цвет нашему item
            if (note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            //Если путь изображения пустой то скрывааем картинку
            if (note.getImagePath() == null || note.getImagePath().equals("")){
                roundedImageView.setVisibility(View.GONE);
            } else {
                //Если путь изображения не пустой,то устанавливаем картинку в itemView по path(пути)
                roundedImageView.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                roundedImageView.setVisibility(View.VISIBLE);
            }

        }
    }


    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view  = LayoutInflater.from(mContext).inflate(R.layout.note_item,parent,false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        final Note currentNote = notes.get(position);

        holder.bind(currentNote);

        //По нажатию на наш itemView
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onNoteClicked(currentNote,position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void searchNotes(final String searchWord){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchWord.trim().isEmpty()){
                    notes = noteSource;
                } else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : noteSource){
                        //Если Title или SubTitle или Текст заметки совпадают со всемии которые есть (наверное)
                        //КОРОЧЕ это поиск по title или subtitle или notetext
                        if (note.getTitle().toLowerCase().contains(searchWord.toLowerCase())
                                || note.getSubTitle().toLowerCase().contains(searchWord.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchWord.toLowerCase())){
                            //Если чтото их этого совпадает,то доавляем в наш Temp arraylist
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        },500);

    }

    public void cancelTimer(){
        if (timer != null){
            timer.cancel();
        }
    }

}
