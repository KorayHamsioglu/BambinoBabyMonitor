package com.example.bambinobabymonitor;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class OfflineParentActivity extends AppCompatActivity {

    String _address;
    int _port;
    String _name;
    Thread _listenThread;
    EditText editTextIP,editTextPort;
    Button buttonStartAudio;

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
        setContentView(R.layout.activity_offline_parent);

      //  final TextView connectedText = (TextView) findViewById(R.id.connectedTo);
      //  connectedText.setText(_name);

      //  final TextView statusText = (TextView) findViewById(R.id.textStatus);
       // statusText.setText(R.string.listening);

        editTextIP=(EditText) findViewById(R.id.editTextIP);
        editTextPort=findViewById(R.id.editTextPort);
        buttonStartAudio=findViewById(R.id.buttonStartAudio);

        buttonStartAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _address=editTextIP.getText().toString();
                String port=editTextPort.getText().toString();
                _port=Integer.parseInt(port);

                _listenThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            final Socket socket = new Socket(_address, _port);
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

                            OfflineParentActivity.this.runOnUiThread(new Runnable()
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

    @Override
    public void onDestroy()
    {
        _listenThread.interrupt();
        _listenThread = null;

        super.onDestroy();
    }
}