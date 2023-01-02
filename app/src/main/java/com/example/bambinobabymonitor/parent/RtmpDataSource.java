package com.example.bambinobabymonitor.parent;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import net.butterflytv.rtmp_client.RtmpClient;

import java.io.IOException;

public class RtmpDataSource implements DataSource {

    public static class RtmpDataSourceFactory implements DataSource.Factory {

        @Override
        public DataSource createDataSource() {
            return new RtmpDataSource();
        }
    }
    private final RtmpClient rtmpClient;
    private Uri uri;

    public RtmpDataSource() {
        rtmpClient = new RtmpClient();
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        String uriString = dataSpec.uri.toString();
        try {
            rtmpClient.open(uriString, false);
            uri = dataSpec.uri;
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return rtmpClient.read(buffer, offset, readLength);
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() throws IOException {
        rtmpClient.close();
    }
}
