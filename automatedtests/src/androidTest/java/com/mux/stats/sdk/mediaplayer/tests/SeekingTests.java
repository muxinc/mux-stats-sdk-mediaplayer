package com.mux.stats.sdk.mediaplayer.tests;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class SeekingTests extends SeekingTestBase {
  private int contentDurationInMs = 128267;

  @Before
  public void init() {
    if (currentTestName.getMethodName()
        .equalsIgnoreCase("testPlaybackWhenStartingFromThePosition")) {
      playbackStartPosition = contentDurationInMs / 3;
    }
    if (currentTestName.getMethodName()
        .equalsIgnoreCase("testSeekWhilePlayWhenReadyIsFalse")) {
      playbackStartPosition = contentDurationInMs / 3;
      playWhenReady = false;
    }
    super.init();
  }

  /*
   * Test Seeking, event order
   */
  @Test
  public void testSeekingWhilePausedVideoAndAudio() {
    testSeekingWhilePaused();
  }

  @Test
  public void testSeekingWhilePlayingVideoAndAudio() {
    testSeekingWhilePlaying();
  }

  @Test
  public void testSeekWhilePlayWhenReadyIsFalse() {
    try {
      testActivity.waitForActivityToInitialize();
      Thread.sleep(PAUSE_PERIOD_IN_MS);
      resumePlayer();
      Thread.sleep(PLAY_PERIOD_IN_MS * 2);

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", 0)
          .add("seeking", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence().filterNameOut("renditionchange");

      MuxStatsEventSequence.hasPrefix(expected, actual);
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }

  /*
   * We are currently missing a play event in this use case scenario
   */
  @Test
  public void testPlaybackWhenStartingFromThePosition() {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      Thread.sleep(PLAY_PERIOD_IN_MS);

      MuxStatsEventSequence expected = new MuxStatsEventSequence();
      expected
          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("pause", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeking", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("seeked", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("play", MuxStatsEventSequence.DELTA_DONT_CARE)
          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence().filterNameOut("renditionchange");

      MuxStatsEventSequence.hasPrefix(expected, actual);
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }
}
