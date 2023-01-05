package com.example.bambinobabymonitor.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bambinobabymonitor.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {

    ArrayList<String> songsList;
    Context context;

    public MusicListAdapter(ArrayList<String> songsList, Context context) {
        this.songsList = songsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.musiclist_item,parent,false);
        return new MusicListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String songName=songsList.get(position);
        holder.textViewMusicName.setText(songName);

        holder.imageButtonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth firebaseAuth=FirebaseAuth.getInstance();
                String userID=firebaseAuth.getCurrentUser().getUid();
                FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance();
                DatabaseReference databaseReference=firebaseDatabase.getReference().child("Users").child(userID).child("command_music_play");
                databaseReference.setValue(songName);
                holder.imageButtonPlay.setVisibility(View.INVISIBLE);
                holder.imageButtonStop.setVisibility(View.VISIBLE);

            }
        });

        holder.imageButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth firebaseAuth=FirebaseAuth.getInstance();
                String userID=firebaseAuth.getCurrentUser().getUid();
                FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance();
                DatabaseReference databaseReference=firebaseDatabase.getReference().child("Users").child(userID).child("command_music_play");
                databaseReference.setValue("none");
                holder.imageButtonPlay.setVisibility(View.VISIBLE);
                holder.imageButtonStop.setVisibility(View.INVISIBLE);

            }
        });

    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textViewMusicName;
        ImageButton imageButtonPlay,imageButtonStop;
        public ViewHolder(View itemView){
            super(itemView);
            textViewMusicName=itemView.findViewById(R.id.textViewMusicName);
            imageButtonPlay=itemView.findViewById(R.id.musicPlayButton);
            imageButtonStop=itemView.findViewById(R.id.musicStopButton);
        }
    }
}
