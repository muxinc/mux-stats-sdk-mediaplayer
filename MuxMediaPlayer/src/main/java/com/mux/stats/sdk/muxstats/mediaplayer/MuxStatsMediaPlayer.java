package com.mux.stats.sdk.muxstats.mediaplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.mux.stats.sdk.core.events.EventBus;
import com.mux.stats.sdk.core.events.IEvent;
import com.mux.stats.sdk.core.events.playback.EndedEvent;
import com.mux.stats.sdk.core.events.playback.ErrorEvent;
import com.mux.stats.sdk.core.events.playback.PauseEvent;
import com.mux.stats.sdk.core.events.playback.PlayEvent;
import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.SeekedEvent;
import com.mux.stats.sdk.core.events.playback.SeekingEvent;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.util.MuxLogger;
import com.mux.stats.sdk.muxstats.IDevice;
import com.mux.stats.sdk.muxstats.IPlayerListener;
import com.mux.stats.sdk.muxstats.MuxStats;

import java.lang.ref.WeakReference;

public class MuxStatsMediaPlayer extends EventBus implements IPlayerListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {
    protected static final String TAG = "MuxStatsMediaPlayer";

    protected MuxStats muxStats;
    protected WeakReference<MediaPlayer> player;
    protected WeakReference<View> playerView;

    protected Integer sourceWidth;
    protected Integer sourceHeight;
    protected boolean isBuffering;
    protected boolean isPlayerPrepared = false;

    /**
     * This class calls the following methods on {@code player} to set itself as the listener:
     * <ul>
     *     <li>{@link android.media.MediaPlayer#setOnCompletionListener}</li>
     *     <li>{@link android.media.MediaPlayer#setOnErrorListener}</li>
     *     <li>{@link android.media.MediaPlayer#setOnInfoListener}</li>
     *     <li>{@link android.media.MediaPlayer#setOnSeekCompleteListener}</li>
     *     <li>{@link android.media.MediaPlayer#setOnVideoSizeChangedListener}</li>
     * </ul>
     *
     * If {@code player} needs alternative listeners installed for these events, be sure to call
     * the corresponding handler on this class from inside those listeners so that events are
     * properly tracked.
     *
     * TODO: there must be a better way...
     */
    MuxStatsMediaPlayer(Context ctx, MediaPlayer player, String playerName,
                        CustomerPlayerData customerPlayerData,
                        CustomerVideoData customerVideoData) {
        super();
        this.player = new WeakReference<>(player);
        MuxStats.setHostDevice(new MuxDevice(ctx));
        MuxStats.setHostNetworkApi(new MuxNetworkRequest());
        muxStats = new MuxStats(this, playerName, customerPlayerData, customerVideoData);
        addListener(muxStats);
        setMediaPlayerListeners();
    }

    private void setMediaPlayerListeners() {
        if (player != null && player.get() != null) {
            player.get().setOnCompletionListener(this);
            player.get().setOnErrorListener(this);
            player.get().setOnInfoListener(this);
            player.get().setOnSeekCompleteListener(this);
            player.get().setOnVideoSizeChangedListener(this);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (player != null && player.get() != null)
            return player.get().getCurrentPosition();
        return 0;
    }

    @Override
    public String getMimeType() {
        // TODO: Other versions?
        if (Build.VERSION.SDK_INT >= 26 && player != null && player.get() != null
                && player.get().getMetrics() != null) {
            return player.get().getMetrics()
                    .getString(MediaPlayer.MetricsConstants.MIME_TYPE_VIDEO);
        }
        return null;
    }

    @Override
    public Integer getSourceWidth() {
        return sourceWidth;
    }

    @Override
    public Integer getSourceHeight() {
        return sourceHeight;
    }

    @Override
    public Long getSourceDuration() {
        if (isPlayerPrepared && player != null && player.get() != null)
            return Long.valueOf(player.get().getDuration());
        return null;
    }

    @Override
    public boolean isPaused() {
        if (player != null && player.get() != null)
            return !player.get().isPlaying();
        return false;
    }

    @Override
    public boolean isBuffering() {
        return isBuffering;
    }

    @Override
    public int getPlayerViewWidth() {
        if (playerView != null && playerView.get() != null) {
            return playerView.get().getWidth();
        }
        return 0;
    }

    @Override
    public int getPlayerViewHeight() {
        if (playerView != null && playerView.get() != null) {
            return playerView.get().getHeight();
        }
        return 0;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer player, int width, int height) {
        sourceWidth = width;
        sourceHeight = height;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            isBuffering = true;
            return true;
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            isBuffering = false;
            return true;
        } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            dispatch(new PlayingEvent(null));
            return true;
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        dispatch(new EndedEvent(null));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO: fix spurious error -38 when player is initializing
        // https://stackoverflow.com/questions/9008770/media-player-called-in-state-0-error-38-0
        dispatch(new ErrorEvent(null));
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        dispatch(new SeekedEvent(null));
        if (player.get().isPlaying()) {
            dispatch(new PlayingEvent(null));
        }
    }

    public void play() {
        dispatch(new PlayEvent(null));
        if (player.get().isPlaying()) {
            dispatch(new PlayingEvent(null));
        }
    }

    public void pause() {
        dispatch(new PauseEvent(null));
    }

    public void seeking() {
        dispatch(new SeekingEvent(null));
    }

    public void release() {
        muxStats.release();
        muxStats = null;
        player = null;
        playerView = null;
    }

    @Override
    public void dispatch(IEvent event) {
        Log.d("MuxEventDispatch", event.toString());
        super.dispatch(event);
    }

    /**
     * Should be set to true once {@link MediaPlayer#setDataSource} has been called. Should be
     * set to false if {@link MediaPlayer#reset} is called on the encapsulated player.
     */
    public void setIsPlayerPrepared(boolean isPrepared) {
        isPlayerPrepared = isPrepared;
    }

    static class MuxDevice implements IDevice {
        private static final String MEDIA_PLAYER_SOFTWARE = "MediaPlayer";

        private String deviceId;
        private String appName = "";
        private String appVersion = "";

        MuxDevice(Context ctx) {
            deviceId = Settings.Secure.getString(ctx.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            try {
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                appName = pi.packageName;
                appVersion = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                MuxLogger.d(TAG, "could not get package info");
            }
        }

        @Override
        public String getHardwareArchitecture() {
            return Build.HARDWARE;
        }

        @Override
        public String getOSFamily() {
            return "Android";
        }

        @Override
        public String getOSVersion() {
            return Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
        }

        @Override
        public String getManufacturer() {
            return Build.MANUFACTURER;
        }

        @Override
        public String getModelName() {
            return Build.MODEL;
        }

        @Override
        public String getPlayerVersion() {
            return getOSVersion();
        }

        @Override
        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public String getAppName() {
            return appName;
        }

        @Override
        public String getAppVersion() {
            return appVersion;
        }

        @Override
        public String getPluginName() { return BuildConfig.MUX_PLUGIN_NAME; }

        @Override
        public String getPluginVersion() { return BuildConfig.MUX_PLUGIN_VERSION; }

        @Override
        public String getPlayerSoftware() {
            return MEDIA_PLAYER_SOFTWARE;
        }

        @Override
        public void outputLog(String tag, String msg) {
            Log.v(tag, msg);
        }
    }
}
