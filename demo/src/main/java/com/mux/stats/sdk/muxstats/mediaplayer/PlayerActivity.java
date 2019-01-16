package com.mux.stats.sdk.muxstats.mediaplayer;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.MediaController;

import java.io.IOException;

public class PlayerActivity extends Activity implements MediaPlayer.OnPreparedListener,
        SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnVideoSizeChangedListener {
    private static final String DEMO_URI = "https://html5demos.com/assets/dizzy.mp4";
    private static final String TAG = "PlayerActivity";

    private MediaPlayer player;
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
        mediaController.show(0);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // Set size of SurfaceView that holds MediaPlayer.
        // Note: this assumes video is full width and height needs to be scaled.
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
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
        }

        @Override
        public void pause() {
            player.pause();
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
