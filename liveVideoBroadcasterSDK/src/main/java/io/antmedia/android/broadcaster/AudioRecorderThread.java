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
        boolean isCrying = false;
        int environmentVoiceCount = 0;
        int babyVoiceCount = 0;
        boolean isFirst = true;
        int outerCount = 0;
        int i = 0;
        byte[] data;
        int timeCount = 0;
        long tEnd;
        long tStart=System.currentTimeMillis();
        while ((bufferReadResult = audioRecord.read(audioData[i], 0, audioData[i].length)) > 0) {

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
                    if (( (2666 < babyVoice) && (environmentVoice < 1000))  || (environmentVoice + 1333 < babyVoice)) {
                        System.out.println("Voice of Baby: " + babyVoice);
                        System.out.println("Voice of Environment: " + environmentVoice);
                        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("command_voice").setValue(true);
                    } else {
                        System.out.println("Kontrol2");
                        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("command_voice").setValue(false);
                    }
                    babyVoice = 0;
                    babyVoiceCount = 0;
                }
                outerCount++;
                if (outerCount == 1100) {
                    outerCount = 0;
                    babyVoice = 0;
                    babyVoiceCount = 0;
                    environmentVoice=0;
                    environmentVoiceCount=0;
                    databaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("command_voice").setValue(false);
                }


                data = audioData[i];
                Message msg = Message.obtain(audioHandler, AudioHandler.RECORD_AUDIO, data);
                msg.arg1 = bufferReadResult;
                msg.arg2 = (int) (System.currentTimeMillis() - startTime);
                audioHandler.sendMessage(msg);
                i++;
                if (i == 1000) {
                    i = 0;
                }
                if (stopThread) {
                    break;
                }
            }

            Log.d(TAG, "AudioThread Finished, release audioRecord");
            if(timeCount == 10){
                tEnd=System.currentTimeMillis();
                long tDelta=tEnd-tStart;
                tStart = tEnd;
                timeCount = 0;
                System.out.println("1 Ses Ölçme Örneği Süresi: "+tDelta/10);
            }
            timeCount++;
        }
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
