package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.bambinobabymonitor.databinding.ActivityListenBinding;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ListenActivity extends AppCompatActivity {
    ActivityListenBinding activityListenBinding;
    String address,name;
    Thread listenThread;
    int port;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityListenBinding = ActivityListenBinding.inflate(getLayoutInflater());
        View view = activityListenBinding.getRoot();
        setContentView(view);
        final Bundle bundle = getIntent().getExtras();
        address = bundle.getString("address");
        port = bundle.getInt("port");
        name = bundle.getString("name");
        activityListenBinding.connectedTo.setText(name);
        activityListenBinding.textStatus.setText("listening...");
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    final Socket socket = new Socket(address, port);
                    streamAudio(socket);
                }
                catch (UnknownHostException e)
                {
                    //Toast.makeText(ListenActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                catch (IOException e)
                {
                    //Toast.makeText(ListenActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }

                if(Thread.currentThread().isInterrupted() == false)
                {
                    // If this thread has not been interrupted, likely something
                    // bad happened with the connection to the child device. Play
                    // an alert to notify the user that the connection has been
                    // interrupted.

                    ListenActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            activityListenBinding.connectedTo.setText("");

                            activityListenBinding.textStatus.setText("disconnected!");
                        }
                    });
                }
            }
        });
        listenThread.start();

    }

    private void streamAudio(final Socket socket) throws IllegalArgumentException, IllegalStateException, IOException
    {

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
    protected void onDestroy() {
        super.onDestroy();
        listenThread.interrupt();
        listenThread = null;
    }
}