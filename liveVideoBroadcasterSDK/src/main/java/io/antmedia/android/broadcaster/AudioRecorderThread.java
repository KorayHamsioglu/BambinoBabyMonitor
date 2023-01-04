package io.antmedia.android.broadcaster;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Message;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;

import io.antmedia.android.broadcaster.encoder.AudioHandler;

/**
 * Created by mekya on 28/03/2017.
 */

class AudioRecorderThread extends Thread {

    private static final String TAG = AudioRecorderThread.class.getSimpleName();
    private final int mSampleRate;
    private final long startTime;
    private volatile boolean stopThread = false;

    private android.media.AudioRecord audioRecord;
    private AudioHandler audioHandler;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    public AudioRecorderThread(int sampleRate, long recordStartTime, AudioHandler audioHandler) {
        this.mSampleRate = sampleRate;
        this.startTime = recordStartTime;
        this.audioHandler = audioHandler;
    }


    @Override
    public void run() {
        //Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = android.media.AudioRecord
                .getMinBufferSize(mSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
        byte[][] audioData;
        int bufferReadResult;

        audioRecord = new android.media.AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        // divide byte buffersize to 2 to make it short buffer
        audioData = new byte[1000][bufferSize];

        firebaseAuth=FirebaseAuth.getInstance();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference("Users");

        MediaRecorder mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();

        audioRecord.startRecording();

        int environmentVoice = 0;
        int babyVoice = 0;
        int j=0;
        int k=0;

        int i = 0;
        byte[] data;
        while ((bufferReadResult = audioRecord.read(audioData[i], 0, audioData[i].length)) > 0) {
            if (j < 100){
                environmentVoice += mediaRecorder.getMaxAmplitude();
            }else if(j == 100 ){
                environmentVoice /= 100;
                System.out.println("OrtamSESİ :" +environmentVoice);
            }


            if(k < 200){
                babyVoice += mediaRecorder.getMaxAmplitude();
            }else if(k == 200){
                babyVoice /= 200;
                if(environmentVoice + 1000 < babyVoice){
                    //TODO: Push Notification!
                    System.out.println("Uyarı!");
                    databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("command_voice").setValue(true);
                }else{
                    databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("command_voice").setValue(false);
                }
                k = 0;
                babyVoice = 0;
            }
            data = audioData[i];



            Message msg = Message.obtain(audioHandler, AudioHandler.RECORD_AUDIO, data);
            msg.arg1 = bufferReadResult;
            msg.arg2 = (int)(System.currentTimeMillis() - startTime);
            audioHandler.sendMessage(msg);


            i++;
            if (i == 1000) {
                i = 0;
            }
            if (stopThread) {
                break;
            }
            j++;
            k++;
        }

        Log.d(TAG, "AudioThread Finished, release audioRecord");

    }

    public void stopAudioRecording() {

        if (audioRecord != null && audioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
            stopThread = true;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

}
