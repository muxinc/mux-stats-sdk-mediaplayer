package com.mux.stats.sdk.muxstats.mediaplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.events.EventBus;
import com.mux.stats.sdk.core.events.InternalErrorEvent;
import com.mux.stats.sdk.core.events.playback.EndedEvent;
import com.mux.stats.sdk.core.events.playback.PauseEvent;
import com.mux.stats.sdk.core.events.playback.PlayEvent;
import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.RebufferEndEvent;
import com.mux.stats.sdk.core.events.playback.RebufferStartEvent;
import com.mux.stats.sdk.core.events.playback.RenditionChangeEvent;
import com.mux.stats.sdk.core.events.playback.SeekedEvent;
import com.mux.stats.sdk.core.events.playback.SeekingEvent;
import com.mux.stats.sdk.core.events.playback.TimeUpdateEvent;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.util.MuxLogger;
import com.mux.stats.sdk.muxstats.IDevice;
import com.mux.stats.sdk.muxstats.INetworkRequest;
import com.mux.stats.sdk.muxstats.IPlayerListener;
import com.mux.stats.sdk.muxstats.LogPriority;
import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.mux.stats.sdk.muxstats.MuxSDKViewPresentation;
import com.mux.stats.sdk.muxstats.MuxStats;

import java.lang.ref.WeakReference;

public class MuxStatsMediaPlayer extends EventBus implements IPlayerListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnVideoSizeChangedListener {
    protected static final String TAG = "MuxStatsMediaPlayer";

    protected MuxStats muxStats;
    protected WeakReference<MediaPlayer> player;
    protected WeakReference<View> playerView;

    protected WeakReference<MediaPlayer.OnCompletionListener> onCompletionListener;
    protected WeakReference<MediaPlayer.OnErrorListener> onErrorListener;
    protected WeakReference<MediaPlayer.OnInfoListener> onInfoListener;
    protected WeakReference<MediaPlayer.OnPreparedListener> onPreparedListener;
    protected WeakReference<MediaPlayer.OnSeekCompleteListener> onSeekCompleteListener;
    protected WeakReference<MediaPlayer.OnVideoSizeChangedListener> onVideoSizeChangedListener;

    protected int sourceWidth;
    protected int sourceHeight;
    private boolean isSeeking;
    private boolean isPaused;
    protected boolean isBuffering;
    private boolean isRebuffering;
    protected boolean isPlayerPrepared = false;
    private boolean hasStartedPlaying;

    public MuxStatsMediaPlayer(Context ctx, MediaPlayer player, String playerName,
        CustomerPlayerData customerPlayerData,
        CustomerVideoData customerVideoData) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData, new MuxNetworkRequests());
    }

    public MuxStatsMediaPlayer(Context ctx, MediaPlayer player, String playerName,
                        CustomerPlayerData customerPlayerData,
                        CustomerVideoData customerVideoData,
                        INetworkRequest network) {
        super();
        this.player = new WeakReference<>(player);
        MuxStats.setHostDevice(new MuxDevice(ctx));
        MuxStats.setHostNetworkApi(network);
        muxStats = new MuxStats(this, playerName, customerPlayerData, customerVideoData);
        addListener(muxStats);
    }

    public void orientationChange(MuxSDKViewOrientation orientation) {
        muxStats.orientationChange(orientation);
    }

    public void presentationChange(MuxSDKViewPresentation presentation) {
        muxStats.presentationChange(presentation);
    }

    public void videoChange(CustomerVideoData customerVideoData) {
        muxStats.videoChange(customerVideoData);
    }

    public void programChange(CustomerVideoData customerVideoData) {
        muxStats.programChange(customerVideoData);
    }

    public void setPlayerView(View view) {
        playerView = new WeakReference<>(view);
    }

    public void setPlayerSize(int width, int height) {
        muxStats.setPlayerSize(width, height);
    }

    public void setScreenSize(int width, int height) {
        muxStats.setScreenSize(width, height);
    }

    public void error(MuxErrorException e) {
        muxStats.error(e);
    }

    public void setAutomaticErrorTracking(boolean enabled) {
        muxStats.setAutomaticErrorTracking(enabled);
    }

    public void release() {
        muxStats.release();
        muxStats = null;
        player = null;
        playerView = null;
    }

    // Helper methods to wrap another listener for MediaPlayer events. This allows users of
    // this class to chain it as a listener.

    public MediaPlayer.OnCompletionListener getOnCompletionListener (
            MediaPlayer.OnCompletionListener listener) {
        onCompletionListener = new WeakReference<>(listener);
        return this;
    }

    public MediaPlayer.OnErrorListener getOnErrorListener(MediaPlayer.OnErrorListener listener) {
        onErrorListener = new WeakReference<>(listener);
        return this;
    }

    public MediaPlayer.OnInfoListener getOnInfoListener(MediaPlayer.OnInfoListener listener) {
        onInfoListener = new WeakReference<>(listener);
        return this;
    }

    public MediaPlayer.OnPreparedListener getOnPreparedListener(
            MediaPlayer.OnPreparedListener listener) {
        onPreparedListener = new WeakReference<>(listener);
        return this;
    }

    public MediaPlayer.OnSeekCompleteListener getOnSeekCompleteListener(
            MediaPlayer.OnSeekCompleteListener listener) {
        onSeekCompleteListener = new WeakReference<>(listener);
        return this;
    }

    public MediaPlayer.OnVideoSizeChangedListener getOnVideoSizeChangedListener(
            MediaPlayer.OnVideoSizeChangedListener listener) {
        onVideoSizeChangedListener = new WeakReference<>(listener);
        return this;
    }


    // IPlayerListener implementation
    @Override
    public long getCurrentPosition() {
        if (isPlayerPrepared && player != null && player.get() != null) {
            return player.get().getCurrentPosition();
        }
        return 0;
    }

    @Override
    public String getMimeType() {
        // TODO: Other versions?
        if (Build.VERSION.SDK_INT >= 26
                && isPlayerPrepared && player != null && player.get() != null
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

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Integer getSourceAdvertisedBitrate() {
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Float getSourceAdvertisedFramerate() {
        return null;
    }

    @Override
    public Long getSourceDuration() {
        if (isPlayerPrepared && player != null && player.get() != null) {
            return (long) player.get().getDuration();
        }
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public boolean isPaused() {
        if (isPlayerPrepared && player != null && player.get() != null)
            return !player.get().isPlaying();
        return false;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public boolean isBuffering() {
        return isBuffering;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public int getPlayerViewWidth() {
        if (playerView != null && playerView.get() != null) {
            return playerView.get().getWidth();
        }
        return 0;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public int getPlayerViewHeight() {
        if (playerView != null && playerView.get() != null) {
            return playerView.get().getHeight();
        }
        return 0;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Long getPlayerProgramTime() {
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Long getPlayerManifestNewestTime() {
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Long getVideoHoldback() {
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Long getVideoPartHoldback() {
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Long getVideoPartTargetDuration() {
        return null;
    }

    /**
     * This method is not supported for AMP
     * @return always null
     */
    @Override
    public Long getVideoTargetDuration() {
        return null;
    }

    // MediaPlayer.OnVideoSizeChangedListener implementation
    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (onVideoSizeChangedListener != null && onVideoSizeChangedListener.get() != null) {
            onVideoSizeChangedListener.get().onVideoSizeChanged(mp, width, height);
        }

        boolean changed = sourceWidth != width && sourceHeight != height;

        sourceWidth = width;
        sourceHeight = height;

        if(changed) {
            dispatch(new RenditionChangeEvent(null));
        }
    }

    // MediaPlayer.OnInfoListener implementation
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (onInfoListener != null && onInfoListener.get() != null) {
            onInfoListener.get().onInfo(mp, what, extra);
        }

        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            isBuffering = true;
            if(!isSeeking && hasStartedPlaying) {
                isRebuffering = true;
                dispatch(new RebufferStartEvent(null));
            }
            return true;
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            isBuffering = false;
            if(isRebuffering) {
                isRebuffering = false;
                dispatch(new RebufferEndEvent(null));
                if(mp.isPlaying()) {
                    isPaused = false;
                    dispatch(new PlayingEvent(null));
                }
            }
            return true;
        } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            hasStartedPlaying = true;
            return true;
        }
        return false;
    }

    // MediaPlayer.OnCompletionListener implementation
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (onCompletionListener != null && onCompletionListener.get() != null) {
            onCompletionListener.get().onCompletion(mp);
        }

        if(!isPaused) {
            dispatch(new PauseEvent(null));
        }
        dispatch(new EndedEvent(null));
    }

    // MediaPlayer.OnError implementation
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // MediaPlayer is now in an error state, and it is invalid to call many of its methods.
        isPlayerPrepared = false;

        if (onErrorListener != null && onErrorListener.get() != null) {
            onErrorListener.get().onError(mp, what, extra);
        }

        String message;
        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:
                message = "MEDIA_ERROR_IO";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                message = "MEDIA_ERROR_MALFORMED";
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                message = "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                message = "MEDIA_ERROR_TIMED_OUT";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                message = "MEDIA_ERROR_UNSUPPORTED";
                break;
            default:
                message = "unknown";
        }

        dispatch(new InternalErrorEvent(what, message));
        return false;
    }

    // MediaPlayer.OnSeekCompleteListener implementation
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        isSeeking = false;

        if (onSeekCompleteListener != null && onSeekCompleteListener.get() != null) {
            onSeekCompleteListener.get().onSeekComplete(mp);
        }

        isBuffering = false;
        dispatch(new SeekedEvent(null));
        dispatch(new TimeUpdateEvent(null));
        if (player.get().isPlaying()) {
            isPaused = false;
            dispatch(new PlayingEvent(null));
        }
    }

    // MediaPlayer.OnPreparedListener implementation
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (onPreparedListener != null && onPreparedListener.get() != null) {
            onPreparedListener.get().onPrepared(mp);
        }

        isPlayerPrepared = true;
    }

    /**
     * Invoke this method just after {@link MediaPlayer#start()} is called.
     */
    public void play() {
        isPaused = false;
        dispatch(new PlayEvent(null));
        if (player.get().isPlaying()) {
            dispatch(new PlayingEvent(null));
        }
    }

    /**
     * Invoke this method just after {@link MediaPlayer#pause()} is called.
     */
    public void pause() {
        if(!isPaused) {
            isPaused = true;
            dispatch(new PauseEvent(null));
        }
    }

    /**
     * Invoke this method just after {@link MediaPlayer#seekTo(int)} is called.
     */
    public void seeking() {
        isSeeking = true;
        isBuffering = true;

        if(!hasStartedPlaying) {
            dispatch(new PlayEvent(null));
        }

        if(!isPaused) {
            isPaused = true;
            dispatch(new PauseEvent(null));
        }

        dispatch(new SeekingEvent(null));
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

        static final String CONNECTION_TYPE_CELLULAR = "cellular";
        static final String CONNECTION_TYPE_WIFI = "wifi";
        static final String CONNECTION_TYPE_WIRED = "wired";
        static final String CONNECTION_TYPE_OTHER = "other";

        protected WeakReference<Context> contextRef;

        private String deviceId;
        private String appName = "";
        private String appVersion = "";

        MuxDevice(Context ctx) {
            contextRef = new WeakReference<>(ctx);
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
        public String getNetworkConnectionType() {
            // Checking internet connectivity
            Context context = contextRef.get();
            if (context == null) {
                return null;
            }
            ConnectivityManager connectivityMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = null;
            if (connectivityMgr != null) {
                activeNetwork = connectivityMgr.getActiveNetworkInfo();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    NetworkCapabilities nc = connectivityMgr
                            .getNetworkCapabilities(connectivityMgr.getActiveNetwork());
                    if (nc == null) {
                        MuxLogger.d(TAG, "ERROR: Failed to obtain NetworkCapabilities manager !!!");
                        return null;
                    }
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return CONNECTION_TYPE_WIRED;
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return CONNECTION_TYPE_WIFI;
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return CONNECTION_TYPE_CELLULAR;
                    } else {
                        return CONNECTION_TYPE_OTHER;
                    }
                } else {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        return CONNECTION_TYPE_WIRED;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        return CONNECTION_TYPE_WIFI;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        return CONNECTION_TYPE_CELLULAR;
                    } else {
                        return CONNECTION_TYPE_OTHER;
                    }
                }
            }
            return null;
        }

        @Override
        public long getElapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        @Override
        public void outputLog(LogPriority logPriority, String tag, String msg) {
            switch (logPriority) {
                case ERROR:
                    Log.e(tag, msg);
                    break;
                case WARN:
                    Log.w(tag, msg);
                    break;
                case INFO:
                    Log.i(tag, msg);
                    break;
                case DEBUG:
                    Log.d(tag, msg);
                    break;
                case VERBOSE:
                default: // fall-through
                    Log.v(tag, msg);
                    break;
            }
        }

        @Override
        public void outputLog(String tag, String msg) {
            Log.v(tag, msg);
        }
    }
}
