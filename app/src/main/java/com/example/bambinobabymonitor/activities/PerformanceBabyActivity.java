package com.example.bambinobabymonitor.activities;

import static android.content.ContentValues.TAG;

import static java.lang.System.out;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import com.example.bambinobabymonitor.AudioModel;
import com.example.bambinobabymonitor.R;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class PerformanceBabyActivity extends AppCompatActivity {

    NsdManager _nsdManager;
    NsdManager.RegistrationListener _registrationListener;
    Thread _serviceThread;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    String parentPlayerID;
    private MediaPlayer mediaPlayer;
    private  String path;
    private static final String ONESIGNAL_APP_ID = "c45ba6ea-96f5-4070-82fb-030cc886e141";

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

    private void serviceConnection(Socket socket) throws IOException {
        PerformanceBabyActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //  final TextView statusText = (TextView) findViewById(R.id.textStatus);
                // statusText.setText(R.string.streaming);
            }
        });

        final int frequency = 11025;
        final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        final int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);



        String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();
        firebaseFirestore=FirebaseFirestore.getInstance();
        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},100);
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                frequency, channelConfiguration,
                audioEncoding, bufferSize);
        // mediaRecorder=new MediaRecorder();
        final int byteBufferSize = bufferSize*2;
        final byte[] buffer = new byte[byteBufferSize];

        MediaRecorder mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");

        try {
            final OutputStream out = socket.getOutputStream();
            audioRecord.startRecording();
            mediaRecorder.prepare();
            mediaRecorder.start();

            //  mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //mediaRecorder.prepare();
            //mediaRecorder.start();


            socket.setSendBufferSize(byteBufferSize);
            Log.d(TAG, "Socket send buffer size: " + socket.getSendBufferSize());

            int environmentVoice = 0;
            int babyVoice = 0;
            int environmentVoiceCount = 0;
            int babyVoiceCount = 0;
            int outerCount = 0;
            while (Thread.currentThread().isInterrupted() == false) {
                final int read = audioRecord.read(buffer, 0, bufferSize);
                // System.out.println(mediaRecorder.getMaxAmplitude());
                out.write(buffer, 0, read);

                if (environmentVoiceCount < 100) {
                    environmentVoice += mediaRecorder.getMaxAmplitude();
                    environmentVoiceCount++;
                } else if (environmentVoiceCount == 100) {
                    environmentVoice /= 100;
                    System.out.println("The Environment Voice: " + environmentVoice);
                    environmentVoiceCount++;
                } else {
                    if (babyVoiceCount < 200) {
                        babyVoice += mediaRecorder.getMaxAmplitude();
                        babyVoiceCount++;
                    } else if (babyVoiceCount == 200) {
                        babyVoice /= 200;
                        if (((2666 < babyVoice) && (environmentVoice < 1000)) || (environmentVoice + 1333 < babyVoice)) {
                            System.out.println("Voice of Baby: " + babyVoice);
                            System.out.println("Voice of Environment: " + environmentVoice);
                            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()){
                                        DocumentSnapshot documentSnapshot=task.getResult();
                                        if(documentSnapshot.exists()){
                                            parentPlayerID=documentSnapshot.getString("parentPlayerID");
                                            System.out.println("PARENTPLAYER ID: " +parentPlayerID);
                                            try {
                                                System.out.println("ONCE");
                                                OneSignal.postNotification(new JSONObject("{'contents':{'en': 'Bebeğiniz Ağlıyor!'}, 'include_player_ids': ['"+parentPlayerID+"']}"),null);

                                            } catch (JSONException e) {
                                                System.out.println("SONRA");
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            });

                        } else {
                            System.out.println("Kontrol2");

                        }
                        babyVoice = 0;
                        babyVoiceCount = 0;
                    }
                    outerCount++;
                    if (outerCount == 1100) {
                        outerCount = 0;
                        babyVoice = 0;
                        babyVoiceCount = 0;
                        environmentVoice = 0;
                        environmentVoiceCount = 0;
                        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("command_voice").setValue(false);
                    }


                }
            }
        }
        finally
        {
            audioRecord.stop();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_baby);

        _nsdManager = (NsdManager)this.getSystemService(Context.NSD_SERVICE);

        firebaseAuth=FirebaseAuth.getInstance();
        String userID=firebaseAuth.getCurrentUser().getUid();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference().child("Users").child(userID);

        this.registerReceiver(this.batteryLevelReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        AudioManager audioManager= (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        databaseReference.child("volume_level").setValue(volume_level);
        databaseReference.child("command_music_play").setValue(-2);
        databaseReference.child("is_paused").setValue(false);

        _serviceThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while(Thread.currentThread().isInterrupted() == false)
                {
                    ServerSocket serverSocket = null;

                    try
                    {
                        // Initialize a server socket on the next available port.
                        serverSocket = new ServerSocket(0);

                        // Store the chosen port.
                        final int localPort = serverSocket.getLocalPort();
                        final WifiManager wifiManager =
                                (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                        final WifiInfo info = wifiManager.getConnectionInfo();
                        final int address = info.getIpAddress();
                        final String ipAddress = Formatter.formatIpAddress(address);
                        databaseReference.child("Connection").child("ip_address").setValue(ipAddress);
                        databaseReference.child("Connection").child("port").setValue(localPort);
                        // Register the service so that parent devices can
                        // locate the child device
                        registerService(localPort);

                        // Wait for a parent to find us and connect
                        Socket socket = serverSocket.accept();
                        Log.i(TAG, "Connection from parent device received");

                        // We now have a client connection.
                        // Unregister so no other clients will
                        // attempt to connect
                        serverSocket.close();
                        serverSocket = null;
                        unregisterService();

                        try
                        {
                            serviceConnection(socket);
                        }
                        finally
                        {
                            socket.close();
                        }
                    }
                    catch(IOException e)
                    {
                        Log.e(TAG, "Connection failed", e);
                    }

                    // If an exception was thrown before the connection
                    // could be closed, clean it up
                    if(serverSocket != null)
                    {
                        try
                        {
                            serverSocket.close();
                        }
                        catch (IOException e)
                        {
                            Log.e(TAG, "Failed to close stray connection", e);
                        }
                        serverSocket = null;
                    }
                }
            }
        });
        _serviceThread.start();

        final TextView addressText = (TextView) findViewById(R.id.textViewIP);

        // Use the application context to get WifiManager, to avoid leak before Android 5.1
        final WifiManager wifiManager =
                (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final WifiInfo info = wifiManager.getConnectionInfo();
        final int address = info.getIpAddress();
        if(address != 0)
        {
            @SuppressWarnings("deprecation")
            final String ipAddress = Formatter.formatIpAddress(address);
            addressText.setText(ipAddress);
        }
        else
        {
            addressText.setText("No wifi");
        }


        addMusics();
        mediaPlayer=new MediaPlayer();

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

    }

    @Override
    protected void onDestroy()
    {
        Log.i(TAG, "Baby monitor stop");

        unregisterService();

        if(_serviceThread != null)
        {
            _serviceThread.interrupt();
            _serviceThread = null;
        }

        super.onDestroy();
    }

    private void registerService(final int port)
    {
        final NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName("ProtectBabyMonitor");
        serviceInfo.setServiceType("_babymonitor._tcp.");
        serviceInfo.setPort(port);

        _registrationListener = new NsdManager.RegistrationListener()
        {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                final String serviceName = nsdServiceInfo.getServiceName();

                Log.i(TAG, "Service name: " + serviceName);

                PerformanceBabyActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        // final TextView statusText = (TextView) findViewById(R.id.textStatus);
                        // statusText.setText(R.string.waitingForParent);

                        //final TextView serviceText = (TextView) findViewById(R.id.textService);
                        //serviceText.setText(serviceName);

                        final TextView portText = (TextView) findViewById(R.id.textViewPort);
                        portText.setText(Integer.toString(port));
                    }
                });
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                // Registration failed!  Put debugging code here to determine why.
                Log.e(TAG, "Registration failed: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0)
            {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.

                Log.i(TAG, "Unregistering service");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                // Unregistration failed.  Put debugging code here to determine why.

                Log.e(TAG, "Unregistration failed: " + errorCode);
            }
        };

        _nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, _registrationListener);
    }

    private void unregisterService()
    {
        if(_registrationListener != null)
        {
            Log.i(TAG, "Unregistering monitoring service");

            _nsdManager.unregisterService(_registrationListener);
            _registrationListener = null;
        }
    }

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