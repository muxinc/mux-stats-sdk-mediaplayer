package com.mux.stats.sdk.mediaplayer.tests.ui;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.Surface;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.muxstats.INetworkRequest;
import com.mux.stats.sdk.muxstats.mediaplayer.MuxStatsMediaPlayer;
import java.io.IOException;

// Experiment wrapping up the stats and MediaPlayer instance behind something
// that looks like a MediaPlayer

public class MuxStatsWrappedMediaPlayer {
  private MediaPlayer mediaPlayer;
  private MuxStatsMediaPlayer muxStats;

  private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener;
  private MediaPlayer.OnCompletionListener onCompletionListener;
  private MediaPlayer.OnErrorListener onErrorListener;
  private MediaPlayer.OnInfoListener onInfoListener;
  private MediaPlayer.OnPreparedListener onPreparedListener;
  private MediaPlayer.OnSeekCompleteListener onSeekCompleteListener;
  private MediaPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener;

  private boolean buffering;
  private int bufferedPercentage;

  public MuxStatsWrappedMediaPlayer(Context context, String playerName, CustomerPlayerData customerPlayerData, CustomerVideoData customerVideoData, INetworkRequest network) {
    mediaPlayer = new MediaPlayer();
    muxStats = new MuxStatsMediaPlayer(context, mediaPlayer, playerName, customerPlayerData, customerVideoData, network);

    muxStats.setIsPlayerPrepared(false);

    buffering = false;
    bufferedPercentage = 0;

    mediaPlayer.setOnErrorListener((mediaPlayer1, what, extra) -> {
      muxStats.onError(mediaPlayer1, what, extra);

      if(onErrorListener != null) {
        return onErrorListener.onError(mediaPlayer1, what, extra);
      }

      return false;
    });

    mediaPlayer.setOnBufferingUpdateListener((mediaPlayer1, bufferedPercentage) -> {
      MuxStatsWrappedMediaPlayer.this.bufferedPercentage = bufferedPercentage;

      if(onBufferingUpdateListener != null) {
        onBufferingUpdateListener.onBufferingUpdate(mediaPlayer1, bufferedPercentage);
      }
    });

    mediaPlayer.setOnCompletionListener(mediaPlayer1 -> {
      muxStats.onCompletion(mediaPlayer);

      if(onCompletionListener != null) {
        onCompletionListener.onCompletion(mediaPlayer1);
      }
    });

    mediaPlayer.setOnPreparedListener(mediaPlayer1 -> {
      muxStats.setIsPlayerPrepared(true);
      muxStats.onPrepared(mediaPlayer1);

      if(onPreparedListener != null) {
        onPreparedListener.onPrepared(mediaPlayer1);
      }
    });

    mediaPlayer.setOnInfoListener((mediaPlayer1, what, extra) -> {
      switch (what) {
        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
          buffering = true;
          break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
          buffering = false;
          break;
      }

      muxStats.onInfo(mediaPlayer, what, extra);

      if(onInfoListener != null) {
        return onInfoListener.onInfo(mediaPlayer1, what, extra);
      }

      return false;
    });

    mediaPlayer.setOnVideoSizeChangedListener((mediaPlayer1, i, i1) -> {
      muxStats.onVideoSizeChanged(mediaPlayer1, i, i1);

      if(onVideoSizeChangedListener != null) {
        onVideoSizeChangedListener.onVideoSizeChanged(mediaPlayer1, i, i1);
      }
    });

    mediaPlayer.setOnSeekCompleteListener(mediaPlayer1 -> {
      muxStats.onSeekComplete(mediaPlayer1);

      if(onSeekCompleteListener != null) {
        onSeekCompleteListener.onSeekComplete(mediaPlayer1);
      }
    });
  }

  public void setSurface(Surface surface) {
    mediaPlayer.setSurface(surface);
  }

  public void setDataSource(String url) throws IOException {
    mediaPlayer.setDataSource(url);
  }

  public void start() {
    mediaPlayer.start();
    muxStats.play();
  }

  public void pause() {
    mediaPlayer.pause();
    muxStats.pause();
  }

  public void release() {
    if (muxStats != null) {
      muxStats.release();
      muxStats = null;
    }

    if(mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }

  public void seekTo(int position) {
    muxStats.seeking();
    mediaPlayer.seekTo(position);
  }

  public boolean isPlaying() {
    return mediaPlayer.isPlaying();
  }

  public boolean isLooping() {
    return mediaPlayer.isLooping();
  }

  public boolean isBuffering() {
    return buffering;
  }

  public void prepareAsync() {
    mediaPlayer.prepareAsync();
  }

  // Hook for testing
  public MediaPlayer getMediaPlayer() {
    return mediaPlayer;
  }

  // Hook for testing
  public MuxStatsMediaPlayer getMuxStats() {
    return muxStats;
  }

  public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener listener) {
    onBufferingUpdateListener = listener;
  }

  public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
    onCompletionListener = listener;
  }

  public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
    onErrorListener = listener;
  }

  public void setOnInfoListener(MediaPlayer.OnInfoListener listener) {
    onInfoListener = listener;
  }

  public void setOnVideoSizeChangedListener(MediaPlayer.OnVideoSizeChangedListener listener) {
    onVideoSizeChangedListener = listener;
  }

  public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
    onPreparedListener = listener;
  }

  public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener listener) {
    onSeekCompleteListener = listener;
  }

  public int getDuration() {
    return mediaPlayer.getDuration();
  }

  public int getCurrentPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  public long getBufferedPosition() {
    if(mediaPlayer != null) {
      return bufferedPercentage * mediaPlayer.getDuration();
    }

    return 0;
  }
}
