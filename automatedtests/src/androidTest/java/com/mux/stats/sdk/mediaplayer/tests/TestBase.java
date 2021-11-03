package com.mux.stats.sdk.mediaplayer.tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.UiDevice;
import com.mux.stats.sdk.mediaplayer.R;
import com.mux.stats.sdk.mediaplayer.example.mockup.http.SimpleHTTPServer;
import com.mux.stats.sdk.mediaplayer.tests.ui.SimplePlayerActivity;
import java.io.IOException;
import java.util.Collection;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Inclusion from the exoplayer tests, so some comments may be wrong
 */
public abstract class TestBase {
  static final String TAG = "MuxStats";

  @Rule
  public ActivityTestRule<SimplePlayerActivity> activityRule = new ActivityTestRule<>(SimplePlayerActivity.class);

  @Rule
  public TestName currentTestName = new TestName();

  static final int PLAY_PERIOD_IN_MS = 10000;
  static final int PAUSE_PERIOD_IN_MS = 3000;
  static final int WAIT_FOR_NETWORK_PERIOD_IN_MS = 12000;
  protected int runHttpServerOnPort = 5000;
  protected int bandwidthLimitInBitsPerSecond = 25000000;

  // I could not make this work as expected
//    static final int SEEK_PERIOD_IN_MS = 5000;
  protected int sampleFileBitrate = 1083904;
  protected String urlToPlay = "http://localhost:5000/vod.mp4";
  protected String adUrlToPlay;
  // UTC timestamp whenlow network bandwidth was triggered
  long startedJammingTheNetworkAt;
  // Amount of video playback time in player buffer
  private long bufferedTime;
  protected int networkJamPeriodInMs = 10000;
  // This is the number of times the network bandwidth will be reduced,
  // not constantly but each 10 ms a random number between 2 and factor will divide
  // the regular amount of bytes to send.
  // This will stop server completely, this will allow us to easier calculate the rebuffer period
  protected int networkJamFactor = 4;
  protected int waitForPlaybackToStartInMS = 30000;

  protected SimplePlayerActivity testActivity;
  protected Activity currentActivity;
  protected SimpleHTTPServer httpServer;
  protected MockNetworkRequest networkRequest;
  protected long playbackStartPosition = 0;
  protected boolean playWhenReady = true;

  protected boolean testActivityFinished;
  protected View pauseButton;
  protected View playButton;

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

    testActivityFinished = false;
    testActivity.runOnUiThread(() -> {
      playButton = testActivity.findViewById(R.id.activity_simple_test_play_button);
      pauseButton = testActivity.findViewById(R.id.activity_simple_test_pause_button);

      testActivity.setVideoTitle(currentTestName.getMethodName());
      testActivity.setUrlToPlay(urlToPlay);
      if(adUrlToPlay != null) {
        testActivity.setAdTag(adUrlToPlay);
      }
      testActivity.setPlayWhenReady(playWhenReady);

      testActivity.initMediaPlayer();

      testActivity.setPlaybackStartPosition(playbackStartPosition);
      testActivity.startPlayback();
      networkRequest = testActivity.getMockNetwork();
    });
  }

  @After
  public void cleanup() {
    if (httpServer != null) {
      httpServer.kill();
    }
    finishActivity();
//        testScenario.close();
  }

//    public abstract Bundle getActivityOptions();

  public void jamNetwork() {
    testActivity.runOnUiThread(() -> {
      startedJammingTheNetworkAt = System.currentTimeMillis();
      long bufferPosition = testActivity.getBufferedPosition();
      long currentPosition = testActivity.getPlayer().getCurrentPosition();
      bufferedTime = bufferPosition - currentPosition;
      httpServer.jamNetwork(networkJamPeriodInMs, networkJamFactor, true);
    });
  }

  public void exitActivity() {
    testActivity.runOnUiThread(() -> testActivity.finish());
  }

  public void pausePlayer() {
    // Pause video
    testActivity.runOnUiThread(() -> {
      if (pauseButton != null) {
        pauseButton.performClick();
      } else {
        testActivity.getPlayer().pause();
      }
    });
  }

  public void resumePlayer() {
    testActivity.runOnUiThread(() -> {
      if (playButton != null) {
        playButton.performClick();
      } else {
        testActivity.getPlayer().start();
      }
    });
  }

  public void backgroundActivity() {
    UiDevice device = UiDevice.getInstance(getInstrumentation());
    device.pressHome();
  }

  public void finishActivity() {
    try {
      if (!testActivityFinished && testActivity != null) {
        testActivity.finish();
        testActivityFinished = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected Activity getActivityInstance() {
    getInstrumentation().runOnMainSync(() -> {
      Collection<Activity> resumedActivities =
          ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
      for (Activity activity : resumedActivities) {
        currentActivity = activity;
        break;
      }
    });

    return currentActivity;
  }

  protected String getExceptionFullTraceAndMessage(Exception e) {
    String lStackTraceString = "";
    for (StackTraceElement lStEl : e.getStackTrace()) {
      lStackTraceString += lStEl.toString() + "\n";
    }
    lStackTraceString += e.getMessage();
    return lStackTraceString;
  }

  public void triggerTouchEvent(float x, float y) {
    onView(withId(R.id.activity_simple_test_content_view)).perform(touchDownAndUpAction(x, y));
  }

  public static ViewAction touchDownAndUpAction(final float x, final float y) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isDisplayed();
      }

      @Override
      public String getDescription() {
        return "Send touch events.";
      }

      @Override
      public void perform(UiController uiController, final View view) {
        // Get view absolute position
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        // Offset coordinates by view position
        float[] coordinates = new float[]{x + location[0], y + location[1]};
        float[] precision = new float[]{1f, 1f};

        // Send down event, pause, and send up
        MotionEvent down = MotionEvents.sendDown(uiController, coordinates, precision).down;
        uiController.loopMainThreadForAtLeast(200);
        MotionEvents.sendUp(uiController, down, coordinates);
      }
    };
  }
}
