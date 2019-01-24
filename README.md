# Mux Stats SDK MediaPlayer

This is the Mux wrapper around MediaPlayer, build on top of Mux's core Java library,
providing Mux Data performance analytics for applications utilizing
[Android MediaPlayer](https://developer.android.com/guide/topics/media/mediaplayer).

## Releases

Full builds are provided as releases within the repo as versions are released.

## Developer Quick Start

Open this project in Android Studio, and let Gradle run to configure the application.
Run the demo application in the emulator to test out the functionality.

## Integration Guide

### 1. Get the Mux Stats MediaPlayer AAR

Three options for getting the AAR:
1. Download from https://github.com/muxinc/mux-stats-sdk-mediaplayer/releases
2. Open this project in Android Studio and build the release variant of the
`MuxMediaPlayer` module. Find the AAR in
`mux-stats-sdk-mediaplayer/MuxMediaPlayer/build/outputs/aar/MuxMediaPlayer-release.aar`
3. Clone this repo and build the AAR on the command line

        ./gradlew :MuxMediaPlayer:assembleRelease
Find the AAR in `mux-stats-sdk-mediaplayer/MuxMediaPlayer/build/outputs/aar/MuxMediaPlayer-release.aar`


### 2. Import the AAR

We recommend using Android Studio's new module tool which can be
accessed via `File > New > New Module...`. Select the `Import .JAR/.AAR
Package` and then select the `mux.aar` that you downloaded or built.
This should correctly configure the IDE as well as modify your build
configuration (Gradle/Maven).

For an example integration, you can see the demo application within
[this repo](https://github.com/muxinc/mux-stats-sdk-mediaplayer)
which integrates Mux into the ExoPlayer demo application.

### 3. Add the MuxStatsMediaPlayer monitor

Create the `CustomerPlayerData` and `CustomerVideoData` objects as
appropriate for your current playback, and be sure to set your env key.

```java
CustomerPlayerData customerPlayerData = new CustomerPlayerData();
customerPlayerData.setEnvironmentKey("YOUR_ENVIRONMENT_KEY");
CustomerVideoData customerVideoData = new CustomerVideoData();
customerVideoData.setVideoTitle(intent.getStringExtra("YOUR VIDEO TITLE"));
```

Create the `MuxStatsMediaPlayer` object by passing yuour Android
`Context` (typically your `Activity`), the `MediaPlayer` instance, a
player name, and the customer data objects.
```java
import com.mux.stats.sdk.muxstats.mediaplayer.MuxStatsMediaPlayer;
...
muxStats = new MuxStatsMediaPlayer(this, player, "demo-player", customerPlayerData,
                customerVideoData);
```

In order to correctly monitor if the player is full-screen, provide the
screen size to the `MuxStatsMediaPlayer` instance.
```java
Point size = new Point();
getWindowManager().getDefaultDisplay().getSize(size);
muxStats.setScreenSize(size.x, size.y);
```

In order to determine a number of viewer context values as well as track
 the size of the video player, set the player view.
 ```java
 muxStats.setPlayerView(playerView);
 ```

 Finally, when you are destroying the player, call the
 `MuxStatsMediaPlayer.release()` method.
 ```java
 muxStats.release()
 ```

### 4. Add additional MuxStatsMediaPlayer calls

`MediaPlayer` does not provide listener callbacks for all necessary events, so you must add explicit
calls into `MuxStatsMediaPlayer` at the same time that certain `MediaPlayer` methods are invoked:
* [`start`](https://developer.android.com/reference/android/media/MediaPlayer.html#start())
* [`pause`](https://developer.android.com/reference/android/media/MediaPlayer.html#pause())
* [`seekTo`](https://developer.android.com/reference/android/media/MediaPlayer.html#seekTo(int))

For example, in the demo, a
[`MediaController`](https://developer.android.com/reference/android/widget/MediaController) view
is used to control the `MediaPlayer` instance, and the appropriate `MuxStatsMediaPlayer` methods
are invoked in the
[`MediaPlayerControl`](https://developer.android.com/reference/android/widget/MediaController.MediaPlayerControl)
implementation used to link the two instances.
```java
private class MediaPlayerControl implements MediaController.MediaPlayerControl,
        MediaPlayer.OnBufferingUpdateListener {
    @Override
    public void start() {
        if (player != null) {
            player.start();
            muxStats.play();
        }
    }

    @Override
    public void pause() {
        if (player != null) {
            player.pause();
            muxStats.pause();
        }
    }

    @Override
    public void seekTo(int pos) {
        if (player != null) {
            muxStats.seeking();
            player.seekTo(pos);
        }
    }
```

### 5. Test it

After you've integrated, start playing a video in the player you've
integrated with. Then for your viewing session (called a "video view")
to show up in the Mux dashboard, you need to stop watching the video.
In a few minutes you'll see the results in your Mux account. We'll also
email you when the first video view has been recorded.


