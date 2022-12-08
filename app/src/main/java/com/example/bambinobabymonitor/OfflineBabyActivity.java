package com.example.bambinobabymonitor;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.bambinobabymonitor.databinding.ActivityOfflineBabyBinding;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class OfflineBabyActivity extends Activity {
    ActivityOfflineBabyBinding activityOfflineBabyBinding;
    NsdManager _nsdManager;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    boolean permissionToRecordAccepted = false;

    NsdManager.RegistrationListener _registrationListener;

    Thread _serviceThread;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void serviceConnection(Socket socket) throws IOException {
        OfflineBabyActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activityOfflineBabyBinding.textService.setText("Streaming...");

            }
        });

        final int frequency = 11025;
        final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        final int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                frequency, channelConfiguration,
                audioEncoding, bufferSize);

        final int byteBufferSize = bufferSize*2;
        final byte[] buffer = new byte[byteBufferSize];

        try
        {
            audioRecord.startRecording();

            final OutputStream out = socket.getOutputStream();

            socket.setSendBufferSize(byteBufferSize);

            while (socket.isConnected() && Thread.currentThread().isInterrupted() == false)
            {
                final int read = audioRecord.read(buffer, 0, bufferSize);
                out.write(buffer, 0, read);
            }
        }
        finally
        {
            audioRecord.stop();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        activityOfflineBabyBinding = ActivityOfflineBabyBinding.inflate(getLayoutInflater());
        View view = activityOfflineBabyBinding.getRoot();
        setContentView(view);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);


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

                        // Register the service so that parent devices can
                        // locate the child device
                        registerService(localPort);

                        // Wait for a parent to find us and connect
                        Socket socket = serverSocket.accept();

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

                        }
                        serverSocket = null;
                    }
                }
            }
        });
        _serviceThread.start();


        // Use the application context to get WifiManager, to avoid leak before Android 5.1
        final WifiManager wifiManager =
                (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final WifiInfo info = wifiManager.getConnectionInfo();
        final int address = info.getIpAddress();
        if(address != 0)
        {
            @SuppressWarnings("deprecation")
            final String ipAddress = Formatter.formatIpAddress(address);
            activityOfflineBabyBinding.address.setText(ipAddress);
        }
        else
        {
            activityOfflineBabyBinding.address.setText("Disconnect!");
        }

    }

    @Override
    protected void onDestroy()
    {

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



                OfflineBabyActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        activityOfflineBabyBinding.textService.setText("Waiting For Parent!");
                        activityOfflineBabyBinding.textService.setText(serviceName);
                        activityOfflineBabyBinding.port.setText(Integer.toString(port));

                    }
                });
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {

            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0)
            {

            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {

            }
        };

        _nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, _registrationListener);
    }

    /**
     * Uhregistered the service and assigns the listener
     * to null.
     */
    private void unregisterService()
    {
        if(_registrationListener != null)
        {


            _nsdManager.unregisterService(_registrationListener);
            _registrationListener = null;
        }
    }
}