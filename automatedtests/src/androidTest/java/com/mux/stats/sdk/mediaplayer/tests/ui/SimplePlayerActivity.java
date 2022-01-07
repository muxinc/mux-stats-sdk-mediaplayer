package com.mux.stats.sdk.mediaplayer.tests.ui;

import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.mediaplayer.BuildConfig;
import com.mux.stats.sdk.mediaplayer.R;
import com.mux.stats.sdk.mediaplayer.tests.MockNetworkRequest;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimplePlayerActivity extends AppCompatActivity {
  static final String TAG = "SimplePlayerActivity";

  protected static final String PLAYBACK_CHANNEL_ID = "playback_channel";
  protected static final int PLAYBACK_NOTIFICATION_ID = 1;
  protected static final String ARG_URI = "uri_string";
  protected static final String ARG_TITLE = "title";
  protected static final String ARG_START_POSITION = "start_position";

  String videoTitle = "Test Video";
  String urlToPlay;

  MediaPlayer tempPlayer; // Should only be used while initting
  MuxStatsWrappedMediaPlayer mediaPlayer;

  String loadedAdTag;
  boolean playWhenReady = true;
  MockNetworkRequest mockNetwork;
  MediaSessionCompat mediaSessionCompat;
  long playbackStartPosition = 0;

  Lock activityLock = new ReentrantLock();
  Condition playbackEnded = activityLock.newCondition();
  Condition playbackStarted = activityLock.newCondition();
  Condition playbackBuffering = activityLock.newCondition();
  Condition activityClosed = activityLock.newCondition();
  Condition activityInitialized = activityLock.newCondition();
  ArrayList<String> addAllowedHeaders = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Enter fullscreen
    hideSystemUI();
    setContentView(R.layout.activity_simple_test);
    disableUserActions();

    findViewById(R.id.activity_simple_test_play_button).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if(mediaPlayer != null) {
          mediaPlayer.start();
        }
      }
    });

    findViewById(R.id.activity_simple_test_pause_button).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if(mediaPlayer != null) {
          mediaPlayer.pause();
        }
      }
    });
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    if(tempPlayer != null) {
      // This is because tempPlayer should be nulled when mediaplayer is created
      tempPlayer.release();
      tempPlayer = null;
    }

    if(mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }

    signalActivityClosed();
    super.onDestroy();
  }

  public void initMediaPlayer() {
    // Mux details
    CustomerPlayerData customerPlayerData = new CustomerPlayerData();
    if (BuildConfig.SHOULD_REPORT_INSTRUMENTATION_TEST_EVENTS_TO_SERVER) {
      customerPlayerData.setEnvironmentKey(BuildConfig.INSTRUMENTATION_TEST_ENVIRONMENT_KEY);
    } else {
      customerPlayerData.setEnvironmentKey("YOUR_ENVIRONMENT_KEY");
    }
    CustomerVideoData customerVideoData = new CustomerVideoData();
    customerVideoData.setVideoTitle(videoTitle);
    mockNetwork = new MockNetworkRequest();

    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);

    mediaPlayer = new MuxStatsWrappedMediaPlayer(this, "demo-player", customerPlayerData, customerVideoData, mockNetwork);
    mediaPlayer.setSurface(((SurfaceView) findViewById(R.id.activity_simple_test_content_surface_view)).getHolder().getSurface());

    mediaPlayer.getMuxStats().setScreenSize(size.x, size.y);
    mediaPlayer.getMuxStats().setPlayerView(findViewById(R.id.activity_simple_test_content_surface_view));
//    mediaPlayer.getMuxStats().enableMuxCoreDebug(true, false);

    mediaPlayer.setOnCompletionListener(mediaPlayer1 -> {
      signalPlaybackEnded();

      if(mediaPlayer.isLooping()) {
        signalPlaybackStarted();
      }
    });

    mediaPlayer.setOnInfoListener((mediaPlayer1, what, extra) -> {
      switch (what) {
        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
          signalPlaybackBuffering();
          break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
          signalPlaybackStarted();
          break;
        case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
          signalPlaybackStarted();
          break;
        default:
          // Note there are some interesting things in MediaPlayer.OnInfoListener we could get
          // not mentioned here
          break;
      }

      return false;
    });
  }

  public void startPlayback() {
    if(mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
    }

    try {
      mediaPlayer.setDataSource(urlToPlay);

      // Note the lambda param is not what we're using
      mediaPlayer.setOnPreparedListener(mediaPlayer1 -> {
        if(playbackStartPosition > 0) {
          mediaPlayer.seekTo((int) playbackStartPosition);
          mediaPlayer.setOnSeekCompleteListener(mediaPlayer2 -> {
            if(playWhenReady) {
              mediaPlayer.start();
            }
          });
        } else {
          if (playWhenReady) {
            mediaPlayer.start();
          }
        }
      });

      mediaPlayer.prepareAsync();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public MuxStatsWrappedMediaPlayer getPlayer() {
    return mediaPlayer;
  }

  public void setPlayWhenReady(boolean playWhenReady) {
    if(playWhenReady != this.playWhenReady) {
      this.playWhenReady = playWhenReady;

      if(mediaPlayer != null) {
        if(playWhenReady) {
          if(!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
          }
        } else {
          if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
          }
        }
      }
    }
  }

  public void setVideoTitle(String title) {
    videoTitle = title;
  }

  public void setAdTag(String tag) {
    loadedAdTag = tag;
  }

  public void setUrlToPlay(String url) {
    urlToPlay = url;
  }

  public void setPlaybackStartPosition(long position) {
    playbackStartPosition = position;
  }

  public void hideSystemUI() {
    // Enables regular immersive mode.
    // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
    // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
      View decorView = getWindow().getDecorView();
      decorView.setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
              // Set the content to appear under the system bars so that the
              // content doesn't resize when the system bars hide and show.
              | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              // Hide the nav bar and status bar
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
  }

  // Shows the system bars by removing all the flags
  // except for the ones that make the content appear under the system bars.
  public void showSystemUI() {
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
  }

  public MockNetworkRequest getMockNetwork() {
    return mockNetwork;
  }

  public boolean waitForPlaybackToFinish(long timeoutInMs) {
    try {
      activityLock.lock();
      return playbackEnded.await(timeoutInMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    } finally {
      activityLock.unlock();
    }
  }

  public void waitForActivityToInitialize() {
    try {
      activityLock.lock();
      activityInitialized.await(5000, TimeUnit.MILLISECONDS);
      activityLock.unlock();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public boolean waitForPlaybackToStart(long timeoutInMs) {
    try {
      activityLock.lock();
      return playbackStarted.await(timeoutInMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    } finally {
      activityLock.unlock();
    }
  }

  public boolean waitForPlaybackToStartBuffering(long timeoutInMs) {
    if (!mediaPlayer.isBuffering()) {
      try {
        activityLock.lock();
        return playbackBuffering.await(timeoutInMs, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
        return false;
      } finally {
        activityLock.unlock();
      }
    }

    return true;
  }

  public void waitForActivityToClose() {
    try {
      activityLock.lock();
      activityClosed.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      activityLock.unlock();
    }
  }

  public void signalPlaybackStarted() {
    activityLock.lock();
    playbackStarted.signalAll();
    activityLock.unlock();
  }

  public void signalPlaybackBuffering() {
    activityLock.lock();
    playbackBuffering.signalAll();
    activityLock.unlock();
  }

  public void signalPlaybackEnded() {
    activityLock.lock();
    playbackEnded.signalAll();
    activityLock.unlock();
  }

  public void signalActivityClosed() {
    activityLock.lock();
    activityClosed.signalAll();
    activityLock.unlock();
  }

  private void disableUserActions() {
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
  }

  private void enableUserActions() {
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
  }

  public long getBufferedPosition() {
    return mediaPlayer.getBufferedPosition();
  }
}
