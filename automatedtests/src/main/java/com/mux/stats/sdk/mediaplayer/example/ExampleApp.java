package com.mux.stats.sdk.mediaplayer.example;

import android.app.Application;
import com.mux.stats.sdk.mediaplayer.example.mockup.http.SimpleHTTPServer;

public class ExampleApp extends Application {
  protected SimpleHTTPServer httpServer;
  protected int runHttpServerOnPort = 5000;
  protected int bandwidthLimitInBitsPerSecond = 1500000;
  public static final String URL_TO_PLAY = "http://localhost:5000/hls/google_glass/playlist.m3u8";

  @Override
  public void onCreate() {
    super.onCreate();

//    try {
//      httpServer = new SimpleHTTPServer(this, runHttpServerOnPort, bandwidthLimitInBitsPerSecond);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }
}
