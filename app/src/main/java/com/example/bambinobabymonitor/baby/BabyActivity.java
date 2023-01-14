package com.example.bambinobabymonitor.baby;

import static com.example.bambinobabymonitor.activities.MainActivity.RTMP_BASE_URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bambinobabymonitor.AudioModel;
import com.example.bambinobabymonitor.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.antmedia.android.broadcaster.ILiveVideoBroadcaster;
import io.antmedia.android.broadcaster.LiveVideoBroadcaster;
import io.antmedia.android.broadcaster.utils.Resolution;

public class BabyActivity extends AppCompatActivity {

    private static final String TAG = BabyActivity.class.getSimpleName();
    private ViewGroup mRootView;
    boolean mIsRecording = false;
    boolean mIsMuted = false;
    private Timer mTimer;
    private long mElapsedTime;
    public TimerHandler mTimerHandler;
    private ImageView mSettingsButton,mBroadcastControlButton,mCameraButton;
    private CameraResolutionsFragment mCameraResolutionsDialog;
    private Intent mLiveVideoBroadcasterServiceIntent;
    private TextView mStreamLiveStatus;
    private GLSurfaceView mGLView;
    private ILiveVideoBroadcaster mLiveVideoBroadcaster;
     FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private static final String ONESIGNAL_APP_ID = "c45ba6ea-96f5-4070-82fb-030cc886e141";
    private String parentPlayerID;
    private CoordinatorLayout coordinatorLayout;
    boolean isCameraMuted=false;
    private MediaPlayer mediaPlayer;
    private  String path;


    private BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            firebaseAuth=FirebaseAuth.getInstance();
            String userID=firebaseAuth.getCurrentUser().getUid();
            firebaseDatabase=FirebaseDatabase.getInstance();
            databaseReference=firebaseDatabase.getReference();
            databaseReference.child("Users").child(userID).child("battery_level").setValue(level);
            System.out.println("Battery Level: "+level+"%");
        }
    };



    private ServiceConnection mConnection = new ServiceConnection() {

       @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
           if(mLiveVideoBroadcaster==null){
               System.out.println("NULL");
           }
            LiveVideoBroadcaster.LocalBinder binder = (LiveVideoBroadcaster.LocalBinder) service;
            if (mLiveVideoBroadcaster == null) {
                mLiveVideoBroadcaster = binder.getService();
                mLiveVideoBroadcaster.init(BabyActivity.this, mGLView);
                mLiveVideoBroadcaster.setAdaptiveStreaming(true);
                if(mLiveVideoBroadcaster==null){
                    System.out.println("NULL");
                }
            }
            mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);

        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {

           mLiveVideoBroadcaster = null;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baby);
        // Hide title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        System.out.println(RTMP_BASE_URL);
        //binding on resume not to having leaked service connection
        mLiveVideoBroadcasterServiceIntent = new Intent(this, LiveVideoBroadcaster.class);
        //this makes service do its job until done
        firebaseAuth=FirebaseAuth.getInstance();
        String userID = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore=FirebaseFirestore.getInstance();
        firebaseDatabase=FirebaseDatabase.getInstance();
        mCameraButton=findViewById(R.id.camera_button);
        coordinatorLayout=findViewById(R.id.root_layout);

        this.registerReceiver(this.batteryLevelReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        Random random = new Random();

        AudioManager audioManager= (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);


        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    if(isCameraMuted){
                        isCameraMuted=!isCameraMuted;

                        mCameraButton.setImageResource(R.drawable.video);
                    }else{
                        isCameraMuted=!isCameraMuted;
                        int cartoonChoice = random.nextInt(7);
                        mCameraButton.setImageResource(R.drawable.no_video);
                        mGLView.setBackground(getResources().getDrawable(R.drawable.cartoon_0 +cartoonChoice));
                    }


                    //coordinatorLayout.setBackground();



            }
        });

        //Müzik için

        addMusics();
        mediaPlayer=new MediaPlayer();


        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);
        documentReference.update("babyPlayerID",OneSignal.getDeviceState().getUserId());

        databaseReference=firebaseDatabase.getReference("Users").child(userID);
        databaseReference.child("volume_level").setValue(volume_level);
        databaseReference.child("command_music_play").setValue(-2);
        databaseReference.child("is_paused").setValue(false);
        databaseReference.child("command_music_play").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               // HashMap<String, Object> hashMap= (HashMap<String, Object>) snapshot.getValue();
                int commandMusicPlay= (int) ((long) snapshot.getValue());

                if(commandMusicPlay != -2){

                    databaseReference = FirebaseDatabase.getInstance().getReference();
                    databaseReference = databaseReference.child("Users").child(userID).child("Musics");
                    databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            int count = 0;
                            for(DataSnapshot dataSnapshot: task.getResult().getChildren()){
                                if (commandMusicPlay == count){
                                        if (mediaPlayer.isPlaying()) {
                                                mediaPlayer.stop();

                                            }
                                                mediaPlayer.reset();
                                            try {
                                                path= (String) dataSnapshot.getValue();
                                                mediaPlayer.setDataSource(path);
                                                mediaPlayer.prepare();
                                                mediaPlayer.start();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                }
                                count++;
                            }
                        }
                    });


                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        databaseReference.child("command_voice").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Boolean commandVoice= (Boolean) snapshot.getValue();
                if (commandVoice){
                    documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                DocumentSnapshot documentSnapshot=task.getResult();
                                if(documentSnapshot.exists()){
                                    parentPlayerID=documentSnapshot.getString("parentPlayerID");

                                    try {
                                        OneSignal.postNotification(new JSONObject("{'contents':{'en': 'Bebeğiniz Ağlıyor!'}, 'include_player_ids': ['"+parentPlayerID+"']}"),null);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        databaseReference.child("is_paused").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isPaused= (Boolean) snapshot.getValue();
                if (mediaPlayer.isPlaying()){
                    if (isPaused){
                        mediaPlayer.pause();
                    }
                }else {
                    mediaPlayer.start();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    databaseReference=firebaseDatabase.getReference("Users").child(userID);
                    databaseReference.child("is_paused").setValue(false);

                    databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            Boolean isRepeat= (Boolean) task.getResult().child("is_repeat").getValue();
                            int count= (int) task.getResult().child("Musics").getChildrenCount();
                            if (isRepeat){
                                databaseReference.child("command_music_play").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        int commandMusicPlay= (int) ((long) snapshot.getValue());
                                        databaseReference = FirebaseDatabase.getInstance().getReference();
                                        databaseReference = databaseReference.child("Users").child(userID).child("Musics");
                                        databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                                int count = 0;
                                                for(DataSnapshot dataSnapshot: task.getResult().getChildren()){
                                                    if (commandMusicPlay == count){
                                                        if (mediaPlayer.isPlaying()) {
                                                            mediaPlayer.stop();

                                                        }
                                                        mediaPlayer.reset();
                                                        try {
                                                            path= (String) dataSnapshot.getValue();
                                                            mediaPlayer.setDataSource(path);
                                                            mediaPlayer.prepare();
                                                            mediaPlayer.start();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }

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
                            }else {
                                databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                                        int commandMusicPlay= (int) ((long)task.getResult().child("command_music_play").getValue());
                                        commandMusicPlay=(commandMusicPlay+1)%count;
                                        databaseReference.child("command_music_play").setValue(commandMusicPlay);
                                    }
                                });
                            }
                        }
                    });

                }
            });

            databaseReference.child("volume_level").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int volume= (int) ((long)snapshot.getValue());
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,AudioManager.FLAG_SHOW_UI);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


        //Toast.makeText(this, (firebaseAuth.getCurrentUser().getEmail), Toast.LENGTH_SHORT).show();
        startService(mLiveVideoBroadcasterServiceIntent);




        mTimerHandler = new TimerHandler();


        mRootView = (ViewGroup)findViewById(R.id.root_layout);
        mSettingsButton = (ImageView)findViewById(R.id.settings_button);
        mStreamLiveStatus = (TextView) findViewById(R.id.stream_live_status);

        mBroadcastControlButton = (ImageView) findViewById(R.id.toggle_broadcasting);


        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL activity.
        mGLView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        if (mGLView != null) {
            mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        }
    }

    public void changeCamera(View v) {
        if (mLiveVideoBroadcaster != null) {
            mLiveVideoBroadcaster.changeCamera();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //this lets activity bind
        bindService(mLiveVideoBroadcasterServiceIntent, mConnection, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LiveVideoBroadcaster.PERMISSIONS_REQUEST: {
                if (mLiveVideoBroadcaster.isPermissionGranted()) {
                    mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.RECORD_AUDIO) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE )||  ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE ))    {
                        mLiveVideoBroadcaster.requestPermission();
                    } else {
                        new AlertDialog.Builder(BabyActivity.this)
                                .setTitle(R.string.intent_to_settings)
                                .setMessage(R.string.intent_to_settings_content)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        try {
                                            //Open the specific App Info page:
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                            startActivity(intent);

                                        } catch (ActivityNotFoundException e) {
                                            //e.printStackTrace();

                                            //Open the generic Apps page:
                                            Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                            startActivity(intent);

                                        }
                                    }
                                })
                                .show();
                    }
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        //hide dialog if visible not to create leaked window exception
        if (mCameraResolutionsDialog != null && mCameraResolutionsDialog.isVisible()) {
            mCameraResolutionsDialog.dismiss();
       }
        mLiveVideoBroadcaster.pause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLiveVideoBroadcaster.setDisplayOrientation();
        }

    }

    public void showSetResolutionDialog(View v) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragmentDialog = getSupportFragmentManager().findFragmentByTag("dialog");
        if (fragmentDialog != null) {

            ft.remove(fragmentDialog);
        }

        ArrayList<Resolution> sizeList = mLiveVideoBroadcaster.getPreviewSizeList();


        if (sizeList != null && sizeList.size() > 0) {
            mCameraResolutionsDialog = new CameraResolutionsFragment();

            mCameraResolutionsDialog.setCameraResolutions(sizeList, mLiveVideoBroadcaster.getPreviewSize());
            mCameraResolutionsDialog.show(ft, "resolutiton_dialog");
        }
        else {
            Snackbar.make(mRootView, "No resolution available", Snackbar.LENGTH_LONG).show();
        }

    }

    public void toggleBroadcasting(View v) {
        if(mLiveVideoBroadcaster==null){
        }
        if (!mIsRecording)
        {

            if (mLiveVideoBroadcaster != null) {
                if (!mLiveVideoBroadcaster.isConnected()) {
                   String streamName = firebaseAuth.getCurrentUser().getEmail();
                    Toast.makeText(this, streamName, Toast.LENGTH_SHORT).show();
                      //String streamName=
                    new AsyncTask<String, String, Boolean>() {
                        ContentLoadingProgressBar progressBar;
                        @Override
                        protected void onPreExecute() {
                            progressBar = new ContentLoadingProgressBar(BabyActivity.this);
                           progressBar.show();
                        }

                        @Override
                        protected Boolean doInBackground(String... url) {
                            return mLiveVideoBroadcaster.startBroadcasting(url[0]);

                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            progressBar.hide();
                            mIsRecording = result;
                            if (result) {
                                mStreamLiveStatus.setVisibility(View.VISIBLE);

                                mBroadcastControlButton.setColorFilter(getResources().getColor(R.color.red));
                                mSettingsButton.setVisibility(View.GONE);
                                startTimer();//start the recording duration
                            }
                            else {
                               // Snackbar.make(mRootView, R.string.stream_not_started, Snackbar.LENGTH_LONG).show();

                                triggerStopRecording();
                            }
                        }
                    }.execute(RTMP_BASE_URL + streamName);
                }
                else {
                    //Snackbar.make(mRootView, R.string.streaming_not_finished, Snackbar.LENGTH_LONG).show();
                }
            }
            else {
               // Snackbar.make(mRootView, R.string.oopps_shouldnt_happen, Snackbar.LENGTH_LONG).show();
            }
        }
        else
        {
            triggerStopRecording();
        }

    }

    public void toggleMute(View v) {
        if (v instanceof ImageView) {
            ImageView iv = (ImageView) v;
            mIsMuted = !mIsMuted;
            mLiveVideoBroadcaster.setAudioEnable(!mIsMuted);
            iv.setImageDrawable(getResources()
                    .getDrawable(mIsMuted ? R.drawable.no_microphone : R.drawable.microphone));
        }
    }

    public void triggerStopRecording() {
        if (mIsRecording) {
            mBroadcastControlButton.setColorFilter(getResources().getColor(R.color.green));

            mStreamLiveStatus.setVisibility(View.GONE);
            mStreamLiveStatus.setText("R.string.live_indicator");
            mSettingsButton.setVisibility(View.VISIBLE);

            stopTimer();
            mLiveVideoBroadcaster.stopBroadcasting();
        }

        mIsRecording = false;
    }

    //This method starts a mTimer and updates the textview to show elapsed time for recording
    public void startTimer() {

        if(mTimer == null) {
            mTimer = new Timer();
        }

        mElapsedTime = 0;
        mTimer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                mElapsedTime += 1; //increase every sec
                mTimerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();

                if (mLiveVideoBroadcaster == null || !mLiveVideoBroadcaster.isConnected()) {
                    mTimerHandler.obtainMessage(TimerHandler.CONNECTION_LOST).sendToTarget();
                }
            }
        }, 0, 1000);
    }


    public void stopTimer()
    {
        if (mTimer != null) {
            this.mTimer.cancel();
        }
        this.mTimer = null;
        this.mElapsedTime = 0;
    }

    public void setResolution(Resolution size) {
        mLiveVideoBroadcaster.setResolution(size);
    }

    private class TimerHandler extends Handler {
        static final int CONNECTION_LOST = 2;
        static final int INCREASE_TIMER = 1;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INCREASE_TIMER:
                    mStreamLiveStatus.setText(getDurationString((int) mElapsedTime));
                    break;
                case CONNECTION_LOST:
                    triggerStopRecording();
                    new AlertDialog.Builder(BabyActivity.this)
                            .setMessage("R.string.broadcast_connection_lost")
                            .setPositiveButton(android.R.string.yes, null)
                            .show();

                    break;
            }
        }
    }

    public static String getDurationString(int seconds) {

        if(seconds < 0 || seconds > 2000000)//there is an codec problem and duration is not set correctly,so display meaningfull string
            seconds = 0;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if(hours == 0)
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        else
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    public static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }
    //Müzik ekleme realtimeDatabase
    public void addMusics(){

        firebaseAuth=FirebaseAuth.getInstance();
        String userID = firebaseAuth.getCurrentUser().getUid();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference("Users").child(userID);

        String[] projection={
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
        };

        String selection=MediaStore.Audio.Media.IS_MUSIC+"!=0";
        int i=1;
        Cursor cursor=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null);
        while(cursor.moveToNext()){
            AudioModel songData=new AudioModel(cursor.getString(1),cursor.getString(0),cursor.getString(2));
            if(new File(songData.getPath()).exists()){
               databaseReference.child("Musics").child(songData.getTitle()).setValue(songData.getPath());
            }
            i++;
        }
    }
}