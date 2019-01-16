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
        SurfaceHolder.Callback, MediaPlayer.OnCompletionListener {
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

        player = new MediaPlayer();
        mediaController = new MediaController(this);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        try {
            player.setDataSource(DEMO_URI);
            // TODO: make this async
            // https://stackoverflow.com/questions/2961749/mediacontroller-with-mediaplayer
            player.prepare();
            Log.d(TAG, "media player prepared");
        } catch (IOException e) {
            Log.d(TAG, "player unable to load uri " + e.toString());
        }
        playerView = findViewById(R.id.video_view);
        playerView.getHolder().addCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.release();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mediaController.show();
        return super.onTouchEvent(event);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
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

        int videoWidth = player.getVideoWidth();
        int videoHeight = player.getVideoHeight();
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
        layoutParams.width = screenWidth;
        layoutParams.height = (int) (((float)videoHeight / (float)videoWidth) * (float)screenWidth);
        playerView.setLayoutParams(layoutParams);
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

    private class MediaPlayerControl implements MediaController.MediaPlayerControl {
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
            return 0;
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
    }
}
