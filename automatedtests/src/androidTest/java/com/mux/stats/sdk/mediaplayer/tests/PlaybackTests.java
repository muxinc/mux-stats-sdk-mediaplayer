package com.mux.stats.sdk.mediaplayer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.PlayerData;
import com.mux.stats.sdk.muxstats.mediaplayer.MuxStatsMediaPlayer;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PlaybackTests extends TestBase {

  public static final String TAG = "playbackTest";
  static final String secondVideoToPlayUrl = "http://localhost:5000/hls/google_glass/playlist.m3u8";

  @Before
  public void init() {
    if (currentTestName.getMethodName().equalsIgnoreCase("testRebufferingAndStartupTime")) {
      // Reduce bandwidth so that rebuffering actually happens
      bandwidthLimitInBitsPerSecond = 1500000;
    }
    super.init();
  }

  @Test
  public void testVideoChange() {
    testVideoChange(false, true);
  }

  @Test
  public void testProgramChange() {
    testVideoChange(true, false);
  }

  public void testVideoChange(boolean programChange, boolean videoChange) {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      Thread.sleep(PLAY_PERIOD_IN_MS);
      // Video started, do video change, we expect to see fake rebufferstart
      testActivity.runOnUiThread(new Runnable() {
        public void run() {
          CustomerVideoData customerVideoData = new CustomerVideoData();
          customerVideoData.setVideoTitle(currentTestName.getMethodName()
              + "_title_2");
          MuxStatsMediaPlayer muxStats = testActivity.getPlayer().getMuxStats();
          if (videoChange) {
            muxStats.videoChange(customerVideoData);
          }
          if (programChange) {
            muxStats.programChange(customerVideoData);
          }
          testActivity.setUrlToPlay(secondVideoToPlayUrl);
          testActivity.startPlayback();
        }
      });
      Thread.sleep(PLAY_PERIOD_IN_MS);

      MuxStatsEventSequence sequence = networkRequest.getEventsAsSequence();
      List<String> failures = sequence.validate();
      if(!failures.isEmpty()) {
        fail("Invalid event sequence "+failures+"\n"+sequence.toString());
      }

      if(sequence.countEventsOfName("rebufferingstart") != 0) {
        fail("Rebuffering found for video or program change");
      }

    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }

  @Test
  public void testEndEvents() {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      // Seek backward, stage 4
      testActivity.runOnUiThread(new Runnable() {
        public void run() {
          int contentDuration = (int) testActivity.getPlayer().getDuration();
          testActivity.getPlayer().seekTo(contentDuration - 2000);
        }
      });
      if (!testActivity.waitForPlaybackToFinish(waitForPlaybackToStartInMS)) {
        fail("Playback did not finish in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      testActivity.finishAffinity();
      Thread.sleep(PAUSE_PERIOD_IN_MS);

      MuxStatsEventSequence sequence = networkRequest.getEventsAsSequence();
      List<String> failures = sequence.validate();
      if(!failures.isEmpty()) {
        fail("Invalid event sequence "+failures);
      }

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", 0)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeking", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("ended", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewend", MuxStatsEventSequence.DELTA_DONT_CARE);

      // We need to ignore the rendition changes in the scope of this test
      MuxStatsEventSequence.compare(expected, sequence.filterNameOut("renditionchange"));

    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }
  /*
   * According to the self validation guid: https://docs.google.com/document/d/1FU_09N3Cg9xfh784edBJpgg3YVhzBA6-bd5XHLK7IK4/edit#
   * We are implementing vod playback scenario.
   */
  @Test
  public void testVodPlayback() {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }

      Thread.sleep(PLAY_PERIOD_IN_MS);
      pausePlayer();
      Thread.sleep(PAUSE_PERIOD_IN_MS);
      resumePlayer();
      Thread.sleep(PLAY_PERIOD_IN_MS);

      testActivity.runOnUiThread(new Runnable() {
        public void run() {
          long currentPlaybackPosition = testActivity.getPlayer().getCurrentPosition();
          testActivity.getPlayer().seekTo((int)currentPlaybackPosition / 2);
        }
      });

      Thread.sleep(PLAY_PERIOD_IN_MS);

      testActivity.runOnUiThread(new Runnable() {
        public void run() {
          long currentPlaybackPosition = testActivity.getPlayer()
              .getCurrentPosition();
          long videoDuration = testActivity.getPlayer().getDuration();
          long seekToInFuture =
              currentPlaybackPosition + ((videoDuration - currentPlaybackPosition) / 2);
          testActivity.getPlayer().seekTo((int) seekToInFuture);
        }
      });

      Thread.sleep(PLAY_PERIOD_IN_MS * 2);

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", 0)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", 10000)
          .add("play", 3000)
          .add("playing", 0)
          .add("pause", 10000)
          .add("seeking", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", 10000)
          .add("seeking", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", 0);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence().filterNameOut("renditionchange");

      MuxStatsEventSequence.compare(expected, actual);

      // Exit the player with back button
//            testScenario.close();
      Log.w(TAG, "See what event should be dispatched on view closed !!!");
      checkFullScreenValue();
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
    Log.e(TAG, "All done !!!");
  }

  @Test
  public void testRebufferingAndStartupTime() {
    try {
      long testStartedAt = System.currentTimeMillis();
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      long expectedStartupTime = System.currentTimeMillis() - testStartedAt;

      // play x seconds
      Thread.sleep(PLAY_PERIOD_IN_MS);
      jamNetwork();
      if(!testActivity.waitForPlaybackToStartBuffering(10000)) {
        fail("Failed to trigger rebuffer state");
      }
      long rebufferStartedAT = System.currentTimeMillis();

      // Wait for rebuffer to complete
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }

      long measuredRebufferPeriod = System.currentTimeMillis() - rebufferStartedAT;
      // play x seconds
      Thread.sleep(PLAY_PERIOD_IN_MS * 2);
//            exitActivity();
//            testScenario.close();

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", 0)
          // TODO look into why the expectedStartupTime doesn't match
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
//          .add("playing", expectedStartupTime)
          .add("rebufferstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          // TODO look into why the measuredRebufferPeriod doesn't match
          .add("rebufferend", MuxStatsEventSequence.DELTA_DONT_CARE)
//          .add("rebufferend", measuredRebufferPeriod)
          .add("playing", 0);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence().filterNameOut("renditionchange");

      MuxStatsEventSequence.compare(expected, actual);
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }

  void checkFullScreenValue() throws JSONException {
    JSONArray events = networkRequest.getReceivedEventsAsJSON();
    for (int i = 0; i < events.length(); i++) {
      JSONObject event = events.getJSONObject(i);
      if (event.has(PlayerData.PLAYER_IS_FULLSCREEN)) {
        assertEquals("Expected player to be in full screen !!!",
            true, event.getBoolean(PlayerData.PLAYER_IS_FULLSCREEN));
        return;
      }
    }
    fail("PlayerData.PLAYER_IS_FULLSCREEN field not present, this is an error !!!");
  }
}
