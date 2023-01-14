package com.example.bambinobabymonitor.activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bambinobabymonitor.R;
import com.example.bambinobabymonitor.adapters.MusicListAdapter;
import com.example.bambinobabymonitor.parent.ParentActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onesignal.OneSignal;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class PerformanceParentActivity extends AppCompatActivity {

    String _address;
    int _port;
    String _name;
    EditText editTextIP,editTextPort;
    Button buttonStartAudio;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    FirebaseFirestore firebaseFirestore;
    Thread _listenThread;
    private ImageView musicListView,lowBatteryImageView,mediumBatteryImageView,highBatteryImageView;
    private static final String ONESIGNAL_APP_ID = "c45ba6ea-96f5-4070-82fb-030cc886e141";





    private void streamAudio(final Socket socket) throws IllegalArgumentException, IllegalStateException, IOException
    {
        Log.i(TAG, "Setting up stream");

        final int frequency = 11025;
        final int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        final int bufferSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        final int byteBufferSize = bufferSize*2;

        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                frequency,
                channelConfiguration,
                audioEncoding,
                bufferSize,
                AudioTrack.MODE_STREAM);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final InputStream is = socket.getInputStream();
        int read = 0;

        audioTrack.play();

        try
        {
            final byte [] buffer = new byte[byteBufferSize];

            while(socket.isConnected() && read != -1 && Thread.currentThread().isInterrupted() == false)
            {
                read = is.read(buffer);

                if(read > 0)
                {
                    audioTrack.write(buffer, 0, read);
                }
            }
        }
        finally
        {
            audioTrack.stop();
            socket.close();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_parent);

        //  final TextView connectedText = (TextView) findViewById(R.id.connectedTo);
        //  connectedText.setText(_name);

        //  final TextView statusText = (TextView) findViewById(R.id.textStatus);
        // statusText.setText(R.string.listening);

        editTextIP=(EditText) findViewById(R.id.editTextIP);
        editTextPort=findViewById(R.id.editTextPort);
        buttonStartAudio=findViewById(R.id.buttonStartAudio);

        lowBatteryImageView = findViewById(R.id.lowBatteryStatusPerformance);
        mediumBatteryImageView = findViewById(R.id.mediumBatteryStatusPerformance);
        highBatteryImageView = findViewById(R.id.highBatteryStatusPerformance);
        musicListView=findViewById(R.id.libraryMusicButtonPerformance);

        firebaseAuth=FirebaseAuth.getInstance();
        String userID=firebaseAuth.getCurrentUser().getUid();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference().child("Users").child(userID);
        firebaseFirestore=FirebaseFirestore.getInstance();

       batteryStatus();

        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        System.out.println("DEVICE ID: "+OneSignal.getDeviceState().getUserId());

        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);
        documentReference.update("parentPlayerID",OneSignal.getDeviceState().getUserId());

        musicListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                musicListPopup();
            }
        });

        buttonStartAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                        databaseReference.child("Connection").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                String ipAddress= (String) task.getResult().child("ip_address").getValue();
                                int port=(int)((long) task.getResult().child("port").getValue());

                                 _listenThread = new Thread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {

                                try
                                {

                                    final Socket socket = new Socket(ipAddress, port);
                                    streamAudio(socket);
                                }
                                catch (UnknownHostException e)
                                {
                                    Log.e(TAG, "Failed to stream audio", e);
                                }
                                catch (IOException e)
                                {
                                    Log.e(TAG, "Failed to stream audio", e);
                                }
                                if(Thread.currentThread().isInterrupted() == false)
                                {
                                    // If this thread has not been interrupted, likely something
                                    // bad happened with the connection to the child device. Play
                                    // an alert to notify the user that the connection has been
                                    // interrupted.
                                    // playAlert();

                                    PerformanceParentActivity.this.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                          /*  final TextView connectedText = (TextView) findViewById(R.id.connectedTo);
                            connectedText.setText("");

                            final TextView statusText = (TextView) findViewById(R.id.textStatus);
                            statusText.setText(R.string.disconnected);
                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(ListenActivity.this)
                                            .setOngoing(false)
                                            .setSmallIcon(R.drawable.listening_notification)
                                            .setContentTitle(getString(R.string.app_name))
                                            .setContentText(getString(R.string.disconnected));
                            _mNotifyMgr.notify(mNotificationId, mBuilder.build());*/
                                        }
                                    });
                                }
                            }
                        });

                        _listenThread.start();

                            }
                        });
            }
        });
    }

    public void musicListPopup(){
        firebaseAuth=FirebaseAuth.getInstance();
        String userID = firebaseAuth.getCurrentUser().getUid();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference().child("Users").child(userID);


        ArrayList<String> songsList=new ArrayList<>();
        databaseReference.child("Musics").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    for (DataSnapshot dataSnapshot: task.getResult().getChildren()){
                        songsList.add(dataSnapshot.getKey());

                    }
                    final Dialog dialog=new Dialog(PerformanceParentActivity.this);
                    dialog.setContentView(R.layout.musiclist_popup);
                    final  int commandMusicPlay;
                    final RecyclerView recyclerViewMusic=dialog.findViewById(R.id.musicRecyclerView);
                    final ImageButton imageButtonClose=dialog.findViewById(R.id.imageButtonClose);
                    final ImageButton imageButtonNext=dialog.findViewById(R.id.imageButtonNext);
                    final ImageButton imageButtonPrevious=dialog.findViewById(R.id.imageButtonPrevious);
                    final ImageButton imageButtonPlay=dialog.findViewById(R.id.imageButtonPlay);
                    final ImageButton imageButtonPause=dialog.findViewById(R.id.imageButtonPause);
                    final ImageButton imageButtonRepeat=dialog.findViewById(R.id.imageButtonRepeat);
                    final ImageButton imageButtonIncrease=dialog.findViewById(R.id.imageButtonIncrease);
                    final ImageButton imageButtonDecrease=dialog.findViewById(R.id.imageButtonDecrease);
                    final TextView textViewVolume=dialog.findViewById(R.id.textViewVolume);


                    final TextView textViewSongName=dialog.findViewById(R.id.textViewSongName);
                    MusicListAdapter musicListAdapter=new MusicListAdapter(songsList,getApplicationContext());
                    recyclerViewMusic.setAdapter(musicListAdapter);
                    textViewSongName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    textViewSongName.setSingleLine(true);
                    textViewSongName.setSelected(true);
                    RecyclerView.LayoutManager layoutManager= new LinearLayoutManager(getApplicationContext());
                    recyclerViewMusic.setLayoutManager(layoutManager);


                    imageButtonIncrease.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    int volume= (int) ((long)task.getResult().child("volume_level").getValue());
                                    if (volume<15){
                                        volume++;
                                        databaseReference.child("volume_level").setValue(volume);
                                    }
                                }
                            });
                        }
                    });

                    imageButtonDecrease.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    int volume= (int) ((long)task.getResult().child("volume_level").getValue());
                                    if (volume>0){
                                        volume--;
                                        databaseReference.child("volume_level").setValue(volume);
                                    }
                                }
                            });
                        }
                    });

                    databaseReference.child("volume_level").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int volume= (int) ((long)snapshot.getValue());
                            textViewVolume.setText(String.valueOf(volume));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            Boolean isPaused= (Boolean) task.getResult().child("is_paused").getValue();
                            Boolean isRepeat= (Boolean) task.getResult().child("is_repeat").getValue();
                            if (isPaused){
                                imageButtonPlay.setVisibility(View.VISIBLE);
                                imageButtonPause.setVisibility(View.GONE);
                            }else{
                                imageButtonPlay.setVisibility(View.GONE);
                                imageButtonPause.setVisibility(View.VISIBLE);
                            }
                            if (isRepeat){
                                imageButtonRepeat.setBackground(getDrawable(R.drawable.repeat));
                            }else{
                                imageButtonRepeat.setBackground(getDrawable(R.drawable.no_repeat));
                            }
                        }
                    });
                    dialog.show();

                    databaseReference.child("command_music_play").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {


                            final int commandMusicPlay= (int) ((long)snapshot.getValue());
                            databaseReference = FirebaseDatabase.getInstance().getReference();
                            databaseReference = databaseReference.child("Users").child(userID);
                            databaseReference.child("Musics").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    int count=0;
                                    for (DataSnapshot dataSnapshot: task.getResult().getChildren()){
                                        if (commandMusicPlay==count){
                                            textViewSongName.setText(dataSnapshot.getKey());

                                            imageButtonPlay.setVisibility(View.GONE);
                                            imageButtonPause.setVisibility(View.VISIBLE);
                                        }

                                        count++;
                                    }
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    imageButtonPlay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.child("is_paused").setValue(false);
                            imageButtonPlay.setVisibility(View.GONE);
                            imageButtonPause.setVisibility(View.VISIBLE);
                        }
                    });

                    imageButtonPause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.child("is_paused").setValue(true);
                            imageButtonPause.setVisibility(View.GONE);
                            imageButtonPlay.setVisibility(View.VISIBLE);
                        }
                    });

                    imageButtonNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if(task.isSuccessful()){
                                        int commandMusicPlay= (int) ((long)task.getResult().child("command_music_play").getValue());
                                        commandMusicPlay=(commandMusicPlay+1)%songsList.size();
                                        databaseReference.child("command_music_play").setValue(commandMusicPlay);
                                        databaseReference.child("is_paused").setValue(false);

                                    }
                                }
                            });
                        }
                    });
                    imageButtonPrevious.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if(task.isSuccessful()){
                                        int commandMusicPlay= (int) ((long)task.getResult().child("command_music_play").getValue());
                                        commandMusicPlay=(((commandMusicPlay-1)%songsList.size()) +songsList.size())% songsList.size();
                                        System.out.println("Command:  "+commandMusicPlay +"  songlist: "+songsList.size()+"  Adapter:  "+musicListAdapter.getItemCount());
                                        databaseReference.child("command_music_play").setValue(commandMusicPlay);
                                        databaseReference.child("is_paused").setValue(false);

                                    }
                                }
                            });
                        }
                    });

                    imageButtonRepeat.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if (task.isSuccessful()){
                                        Boolean isRepeat= (Boolean) task.getResult().child("is_repeat").getValue();
                                        if (isRepeat){
                                            databaseReference.child("is_repeat").setValue(!isRepeat);
                                            imageButtonRepeat.setBackground(getDrawable(R.drawable.no_repeat));
                                        }else{
                                            databaseReference.child("is_repeat").setValue(!isRepeat);
                                            imageButtonRepeat.setBackground(getDrawable(R.drawable.repeat));
                                        }


                                    }
                                }
                            });
                        }
                    });



                    imageButtonClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            dialog.dismiss();
                        }
                    });

                }
            }
        });
    }

    public void batteryStatus(){
        firebaseAuth=FirebaseAuth.getInstance();
        String userID = firebaseAuth.getCurrentUser().getUid();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference().child("Users").child(userID);



        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String,Object> hashMap= (HashMap<String, Object>) snapshot.getValue();
                long batteryLevel= (long) hashMap.get("battery_level");
                System.out.println("BATTERY  "+batteryLevel);
                if (batteryLevel<=25){
                    lowBatteryImageView.setVisibility(View.VISIBLE);
                    mediumBatteryImageView.setVisibility(View.INVISIBLE);
                    highBatteryImageView.setVisibility(View.INVISIBLE);
                }else if(batteryLevel<75){
                    lowBatteryImageView.setVisibility(View.INVISIBLE);
                    mediumBatteryImageView.setVisibility(View.VISIBLE);
                    highBatteryImageView.setVisibility(View.INVISIBLE);

                }else{
                    lowBatteryImageView.setVisibility(View.INVISIBLE);
                    mediumBatteryImageView.setVisibility(View.INVISIBLE);
                    highBatteryImageView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public void onDestroy()
    {
        _listenThread.interrupt();
        _listenThread=null;
        super.onDestroy();
    }
}