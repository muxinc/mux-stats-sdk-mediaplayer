package com.mux.stats.sdk.muxstats.mediaplayer;

import android.app.Activity;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.MediaController;

import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;

import java.io.IOException;

public class PlayerActivity extends Activity implements MediaPlayer.OnPreparedListener,
        SurfaceHolder.Callback, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener {
    private static final String TAG = "PlayerActivity";
    private static final String MUX_ENVIRONMENT_KEY="84c4ps9d15i4ts1pqqgl1pbh4";

    private static final String DEMO_VIDEO_TITLE = "Screens 1080p";
    private static final String DEMO_URI = "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-137.mp4";

    private MediaPlayer player;
    private MuxStatsMediaPlayer muxStats;
    private MediaController mediaController;
    private SurfaceView playerView;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        playerView = findViewById(R.id.video_view);
        playerView.getHolder().addCallback(this);

        player = new MediaPlayer();

        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey(MUX_ENVIRONMENT_KEY);
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle(DEMO_VIDEO_TITLE);
        muxStats = new MuxStatsMediaPlayer(this, player, "demo-player", customerPlayerData,
                customerVideoData);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        muxStats.setScreenSize(size.x, size.y);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnVideoSizeChangedListener(this);
        try {
            player.setDataSource(DEMO_URI);
            player.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "player unable to load uri " + e.toString());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
        }
        if (muxStats != null) {
            muxStats.release();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mediaController != null) {
            mediaController.show();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        muxStats.setIsPlayerPrepared(true);
        // Attach media player controls and display them.
        mediaController = new MediaController(this);
        mediaController.setMediaPlayer(new MediaPlayerControl());
        mediaController.setAnchorView(findViewById(R.id.video_view_container));
        handler.post(new Runnable() {
            @Override
            public void run() {
                mediaController.setEnabled(true);
                mediaController.show(0);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        player.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (muxStats != null) {
            muxStats.onCompletion(mp);
        }
        if (mediaController != null) {
            mediaController.show(0);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (muxStats != null) {
            muxStats.onVideoSizeChanged(mp, width, height);
        }
        // Set size of SurfaceView that holds MediaPlayer.
        // Note: this assumes video is full width and height needs to be scaled.
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
        layoutParams.width = screenWidth;
        layoutParams.height = (int) (((float)height / (float)width) * (float)screenWidth);
        playerView.setLayoutParams(layoutParams);
    }

    private class MediaPlayerControl implements MediaController.MediaPlayerControl,
            MediaPlayer.OnBufferingUpdateListener {
        private int bufferPercent;

        public MediaPlayerControl() {
            player.setOnBufferingUpdateListener(this);
        }

        @Override
        public void start() {
            player.start();
            muxStats.play();
        }

        @Override
        public void pause() {
            player.pause();
            muxStats.pause();
        }

        @Override
        public int getDuration() {
            return player.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            return player.getCurrentPosition();
        }

        @Override
        public void seekTo(int pos) {
            muxStats.seeking();
            player.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return player.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return bufferPercent;
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return player.getAudioSessionId();
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            bufferPercent = percent;
        }
    }
}
