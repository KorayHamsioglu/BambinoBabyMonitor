package com.example.bambinobabymonitor.parent;

import static com.example.bambinobabymonitor.activities.MainActivity.RTMP_BASE_URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bambinobabymonitor.R;
import com.example.bambinobabymonitor.adapters.MusicListAdapter;
import com.google.android.exoplayer2.BuildConfig;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;

public class ParentActivity extends AppCompatActivity implements View.OnClickListener, ExoPlayer.EventListener,
        PlaybackControlView.VisibilityListener {

    public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Handler mainHandler;
    private EventLogger eventLogger;
    private SimpleExoPlayerView simpleExoPlayerView;
    private LinearLayout debugRootView;
    private TextView debugTextView;
    private Button retryButton;
    private ImageView musicListView,lowBatteryImageView,mediumBatteryImageView,highBatteryImageView;

    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private DebugTextViewHelper debugViewHelper;
    private boolean needRetrySource;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;
    private RtmpDataSource.RtmpDataSourceFactory rtmpDataSourceFactory;
    protected String userAgent;
    private View videoStartControlLayout;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private static final String ONESIGNAL_APP_ID = "c45ba6ea-96f5-4070-82fb-030cc886e141";
    private String babyPlayerID;
    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        shouldAutoPlay = true;
        clearResumePosition();
        mediaDataSourceFactory = buildDataSourceFactory(true);
        rtmpDataSourceFactory = new RtmpDataSource.RtmpDataSourceFactory();
        mainHandler = new Handler();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }


        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        batteryStatus();

        firebaseAuth=FirebaseAuth.getInstance();
        firebaseFirestore=FirebaseFirestore.getInstance();

        String userID = firebaseAuth.getCurrentUser().getUid();
        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);


        documentReference.update("parentPlayerID",OneSignal.getDeviceState().getUserId());

       /* documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot=task.getResult();
                    if(documentSnapshot.exists()){
                        babyPlayerID=documentSnapshot.getString("babyPlayerID");
                        try {
                            OneSignal.postNotification(new JSONObject("{'contents':{'en': 'Ebeveyn uygulamaya başarıyla giriş yaptı'}, 'include_player_ids': ['"+babyPlayerID+"']}"),null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });*/

        setContentView(R.layout.activity_parent);
        View rootView = findViewById(R.id.root);
        rootView.setOnClickListener(this);
        debugRootView = (LinearLayout) findViewById(R.id.controls_root);
        debugTextView = (TextView) findViewById(R.id.debug_text_view);
        retryButton = (Button) findViewById(R.id.retry_button);
        musicListView=findViewById(R.id.libraryMusicButton);
        retryButton.setOnClickListener(this);
        lowBatteryImageView = findViewById(R.id.lowBatteryStatus);
        mediumBatteryImageView = findViewById(R.id.mediumBatteryStatus);
        highBatteryImageView = findViewById(R.id.highBatteryStatus);


        //Müzik listesinin açılması
        musicListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                musicListPopup();
            }
        });


        videoStartControlLayout = findViewById(R.id.video_start_control_layout);



        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            play(null);
        } else {
            showToast("R.string.storage_permission_denied");
            finish();
        }
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        simpleExoPlayerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        if (view == retryButton) {
            play(null);
        }
    }

    // PlaybackControlView.VisibilityListener implementation

    @Override
    public void onVisibilityChange(int visibility) {
        debugRootView.setVisibility(visibility);
    }

    // Internal methods

    private void initializePlayer(String rtmpUrl) {
        Intent intent = getIntent();
        boolean needNewPlayer = player == null;
        if (needNewPlayer) {

            boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
            @SimpleExoPlayer.ExtensionRendererMode int extensionRendererMode =
                    useExtensionRenderers()
                            ? (preferExtensionDecoders ? SimpleExoPlayer.EXTENSION_RENDERER_MODE_PREFER
                            : SimpleExoPlayer.EXTENSION_RENDERER_MODE_ON)
                            : SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF;
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl(),
                    null, extensionRendererMode);
            //   player = ExoPlayerFactory.newSimpleInstance(this, trackSelector,
            //           new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),  500, 1500, 500, 1500),
            //           null, extensionRendererMode);
            player.addListener(this);

             eventLogger = new EventLogger(trackSelector);
            player.addListener(eventLogger);
            player.setAudioDebugListener(eventLogger);
            player.setVideoDebugListener(eventLogger);
            player.setMetadataOutput(eventLogger);

            simpleExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
            debugViewHelper = new DebugTextViewHelper(player, debugTextView);
            debugViewHelper.start();
        }
        if (needNewPlayer || needRetrySource) {
            //  String action = intent.getAction();
            Uri[] uris;
            String[] extensions;

            uris = new Uri[1];
            uris[0] = Uri.parse(rtmpUrl);
            extensions = new String[1];
            extensions[0] = "";
            if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
                // The player will be reinitialized if the permission is granted.
                return;
            }
            MediaSource[] mediaSources = new MediaSource[uris.length];
            for (int i = 0; i < uris.length; i++) {
                mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
            }
            MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
                    : new ConcatenatingMediaSource(mediaSources);
            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                player.seekTo(resumeWindow, resumePosition);
            }
            player.prepare(mediaSource, !haveResumePosition, false);
            needRetrySource = false;
        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
                : Util.inferContentType("." + overrideExtension);
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case C.TYPE_OTHER:
                if (uri.getScheme().equals("rtmp")) {
                    return new ExtractorMediaSource(uri, rtmpDataSourceFactory, new DefaultExtractorsFactoryForFLV(),
                            mainHandler, eventLogger);
                }
                else {
                    return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                            mainHandler, eventLogger);
                }
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }


    private void releasePlayer() {
        if (player != null) {
            debugViewHelper.stop();
            debugViewHelper = null;
            shouldAutoPlay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
            //trackSelectionHelper = null;
            eventLogger = null;
        }
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    // ExoPlayer.EventListener implementation

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            showControls();
        }
    }

    @Override
    public void onPositionDiscontinuity() {
        if (needRetrySource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Do nothing.
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        videoStartControlLayout.setVisibility(View.VISIBLE);
        String errorString = null;
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                        errorString = getString(R.string.project_id);
                    } else if (decoderInitializationException.secureDecoderRequired) {
                      /*  errorString = getString(io.antmedia.android.R.string,
                                decoderInitializationException.mimeType);*/
                    } else {
                       /* errorString = getString(R.string.error_no_decoder,
                                decoderInitializationException.mimeType);*/
                    }
                } else {
                    /*errorString = getString(R.string.error_instantiating_decoder,
                            decoderInitializationException.decoderName);*/
                }
            }
        }
        if (errorString != null) {
            showToast(errorString);
        }
        needRetrySource = true;
        if (isBehindLiveWindow(e)) {
            clearResumePosition();
            play(null);
        } else {
            updateResumePosition();
            showControls();
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                    == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                showToast("R.string.error_unsupported_video");
            }
            if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                    == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                showToast("R.string.error_unsupported_audio");
            }
        }
    }

    private void showControls() {
        debugRootView.setVisibility(View.VISIBLE);
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }


    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    public boolean useExtensionRenderers() {
        return BuildConfig.FLAVOR.equals("withExtensions");
    }

    public void play(View view) {
        String URL = RTMP_BASE_URL + firebaseAuth.getCurrentUser().getEmail();
        //String URL = "http://192.168.1.34:5080/vod/streams/test_adaptive.m3u8";
        initializePlayer(URL);
        videoStartControlLayout.setVisibility(View.GONE);
    }
    //Müzik için
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
                    final Dialog dialog=new Dialog(ParentActivity.this);
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


}