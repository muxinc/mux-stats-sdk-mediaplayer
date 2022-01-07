package com.mux.stats.sdk.mediaplayer.tests;

import static org.junit.Assert.fail;

public class SeekingTestBase extends TestBase {

  protected void testSeekingWhilePaused() {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      // play x seconds, stage 1
      Thread.sleep(PLAY_PERIOD_IN_MS);
      pausePlayer();
      Thread.sleep(PAUSE_PERIOD_IN_MS);
      // Seek to the end by triggering touch event
      testActivity.runOnUiThread(() -> {
        long duration = testActivity.getPlayer().getDuration();
        testActivity.getPlayer().seekTo((int) (duration - PLAY_PERIOD_IN_MS));
      });
      Thread.sleep(PLAY_PERIOD_IN_MS);
      finishActivity();

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", 10000)
          .add("seeking", 3000)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence().filterNameOut("renditionchange");

      MuxStatsEventSequence.compare(expected, actual);
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }

  protected void testSeekingWhilePlaying() {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      // play x seconds, stage 1
      Thread.sleep(PLAY_PERIOD_IN_MS);
      // Seek to the end by triggering touch event
      testActivity.runOnUiThread(() -> {
        long duration = testActivity.getPlayer().getDuration();
        testActivity.getPlayer().seekTo((int) (duration - PLAY_PERIOD_IN_MS));
      });
      Thread.sleep(PLAY_PERIOD_IN_MS);
      finishActivity();

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", 10000)
          .add("seeking", 0)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence().filterNameOut("renditionchange");

      MuxStatsEventSequence.compare(expected, actual);
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }
}
