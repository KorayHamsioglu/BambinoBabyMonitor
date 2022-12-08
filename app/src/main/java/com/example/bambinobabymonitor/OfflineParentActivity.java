package com.example.bambinobabymonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.rtp.AudioStream;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.bambinobabymonitor.databinding.ActivityOfflineParentBinding;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class OfflineParentActivity extends AppCompatActivity {
        ActivityOfflineParentBinding activityOfflineParentBinding;
        String addressString,portString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityOfflineParentBinding = ActivityOfflineParentBinding.inflate(getLayoutInflater());
        View view = activityOfflineParentBinding.getRoot();
        setContentView(view);
        activityOfflineParentBinding.connectViaAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addressString =activityOfflineParentBinding.ipAddressField.getText().toString();
                portString = activityOfflineParentBinding.portField.getText().toString();
                if(addressString.length() == 0)
                {
                    Toast.makeText(OfflineParentActivity.this, "Must write the valid address!", Toast.LENGTH_LONG).show();
                    return;
                }

                int port = 0;
                try
                {
                    port = Integer.parseInt(portString);
                }
                catch(NumberFormatException e)
                {
                    Toast.makeText(OfflineParentActivity.this, "Must write the valid port address!", Toast.LENGTH_LONG).show();
                    return;
                }
                connectToChild(addressString,port);
            }
        });
    }


    private void connectToChild(final String address, final int port)
    {
        final Intent intentToListenActivity = new Intent(getApplicationContext(), ListenActivity.class);
        final Bundle bundle = new Bundle();
        bundle.putString("address", address);
        bundle.putInt("port", port);
        bundle.putString("name", address);
        intentToListenActivity.putExtras(bundle);
        startActivity(intentToListenActivity);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Baby Monitor Stop!", Toast.LENGTH_SHORT).show();
    }

}