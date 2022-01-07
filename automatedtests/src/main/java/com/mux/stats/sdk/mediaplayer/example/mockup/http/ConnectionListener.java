package com.mux.stats.sdk.mediaplayer.example.mockup.http;

public interface ConnectionListener {

  void segmentServed(String requestUuid, SegmentStatistics segmentStat);

}
