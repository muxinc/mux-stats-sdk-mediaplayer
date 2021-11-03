package com.mux.stats.sdk.mediaplayer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class RenditionChangeTests extends TestBase {
  public static final String TAG = "renditiontests";

  @Before
  public void init() {
    urlToPlay = "http://localhost:5000/hls/google_glass/playlist.m3u8";
    bandwidthLimitInBitsPerSecond = 200000;

    super.init();
  }

  /*
   * According to the self validation guid: https://docs.google.com/document/d/1FU_09N3Cg9xfh784edBJpgg3YVhzBA6-bd5XHLK7IK4/edit#
   * We are implementing vod playback scenario.
   */
  @Test
  public void testRenditionChangeEvents() {
    try {
      testActivity.waitForActivityToInitialize();
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }

      jamNetwork();
      testActivity.waitForPlaybackToStartBuffering(10000);
      httpServer.jamNetwork(10000, 4, true);
      Thread.sleep(10000);
      httpServer.jamNetwork(1000, 32, false);
      Thread.sleep(1000);
      httpServer.jamNetwork(1000, 24, false);
      Thread.sleep(30000);

      MuxStatsEventSequence actual = networkRequest.getEventsAsSequence();

      if(actual.countEventsOfName("renditionchange") == 0) {
        fail("No renditionchange events generated");
      }
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
    Log.e(TAG, "All done !!!");
  }
}
