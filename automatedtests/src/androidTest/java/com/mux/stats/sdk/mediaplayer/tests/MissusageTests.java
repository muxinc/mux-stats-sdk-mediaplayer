package com.mux.stats.sdk.mediaplayer.tests;

import static org.junit.Assert.fail;

import com.mux.stats.sdk.mediaplayer.example.mockup.http.SimpleHTTPServer;
import com.mux.stats.sdk.mediaplayer.tests.ui.SimplePlayerActivity;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class MissusageTests extends TestBase {

  static final int INIT_MUX_STATS_AFTER = 5000;

  @Before
  public void init() {
    try {
      httpServer = new SimpleHTTPServer(getActivityInstance().getApplicationContext(), runHttpServerOnPort, bandwidthLimitInBitsPerSecond);
    } catch (IOException e) {
      e.printStackTrace();
      // Failed to start server
      fail("Failed to start HTTP server, why !!!");
    }
    try {
      testActivity = (SimplePlayerActivity) getActivityInstance();
    } catch (ClassCastException e) {
      fail("Got wrong activity instance in test init !!!");
    }
    if (testActivity == null) {
      fail("Test activity not found !!!");
    }
  }

  // Now "irrelevant" ?!?!?!?
//  @Test
//  public void testLateStatsInit() {
//    try {
//      // Init test activity but not the Mux stats
//      testActivity.runOnUiThread(() -> {
//        testActivity.setVideoTitle(currentTestName.getMethodName());
//        testActivity.setUrlToPlay(urlToPlay);
//        testActivity.initMediaPlayer();
//        testActivity.startPlayback();
//      });
//      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
//        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
//      }
//      Thread.sleep(INIT_MUX_STATS_AFTER);
//      // Init Mux stats after the playback have started
//      testActivity.runOnUiThread(() -> {
//        testActivity.initMuxStats();
//      });
//      Thread.sleep(INIT_MUX_STATS_AFTER * 2);
//      // This is initialized with the MuxStats, it need to be called after
//      // testActivity.initMuxSats();
//      networkRequest = testActivity.getMockNetwork();
//      // Check if play, playing and etc events are sent
//      MuxStatsEventSequence expected = new MuxStatsEventSequence();
//      expected
//          .add("playerready", MuxStatsEventSequence.DELTA_DONT_CARE)
//          .add("viewstart", MuxStatsEventSequence.DELTA_DONT_CARE)
//          // Note we expect play within 500 ms of viewstart in this case . . . bit of a hack
//          .add("play", 250)
//          .add("playing", MuxStatsEventSequence.DELTA_DONT_CARE);
//
//      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence();
//
//      MuxStatsEventSequence.hasPrefix(expected, actual);
//
//    } catch (Exception e) {
//      fail(getExceptionFullTraceAndMessage(e));
//    }
//
//  }
}
