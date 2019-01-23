package com.mux.stats.sdk.muxstats.mediaplayer;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;

import java.io.IOException;

public class PlayerActivity extends Activity implements MediaPlayer.OnPreparedListener,
        SurfaceHolder.Callback, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener {
    private static final String TAG = "PlayerActivity";
    private static final String MUX_ENVIRONMENT_KEY="84c4ps9d15i4ts1pqqgl1pbh4";

    public static final String VIDEO_TITLE_EXTRA = "video_title";

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

        Intent intent = getIntent();
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey(MUX_ENVIRONMENT_KEY);
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle(intent.getStringExtra(VIDEO_TITLE_EXTRA));
        muxStats = new MuxStatsMediaPlayer(this, player, "demo-player", customerPlayerData,
                customerVideoData);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        muxStats.setScreenSize(size.x, size.y);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(muxStats.getOnCompletionListener(this));
        player.setOnErrorListener(muxStats.getOnErrorListener(this));
        player.setOnInfoListener(muxStats.getOnInfoListener(null));
        player.setOnSeekCompleteListener(muxStats.getOnSeekCompleteListener(null));
        player.setOnVideoSizeChangedListener(muxStats.getOnVideoSizeChangedListener(this));
        try {
            player.setDataSource(this, intent.getData());
            player.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "player unable to load uri " + e.toString());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();

        if (muxStats != null) {
            muxStats.release();
            muxStats = null;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
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
        if (mediaController != null) {
            mediaController.show(0);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
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

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        releasePlayer();
        mediaController.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Playback failed", Toast.LENGTH_LONG).show();
        return false;
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
