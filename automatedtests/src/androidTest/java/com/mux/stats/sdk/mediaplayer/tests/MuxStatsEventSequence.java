package com.mux.stats.sdk.mediaplayer.tests;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * This is a utility class for checking and comparing event sequences sent to MuxCore to see
 * if they are valid and/or match expectations. Moving more of the validation in here ensures we
 * do more validation more often (i.e. on all tests all the time), and our tests are more readable
 * in terms of defining expected behaviour.
 */
public class MuxStatsEventSequence {
  // A delta time value indicating that we compare validly with any other delta value
  public static final long DELTA_DONT_CARE = -1;

  class Event {
    // The muxcore event name like "paused" "playing" etc.
    // Note "hb" events should be filtered out
    String name;
    // The time (in ms) since the previous event in the sequence or DELTA_DONT_CARE if we don't care
    long delta;

    Event(String name, long delta) {
      this.name = name;
      this.delta = delta;
    }

    @Override
    public String toString() {
      if(delta == DELTA_DONT_CARE) {
        return name + " +DONT CARE HOW LONG HERE";
      } else {
        return name + " +" + delta+"ms";
      }
    }
  }

  private ArrayList<Event> events;

  public MuxStatsEventSequence() {
    events = new ArrayList<>();
  }

  public MuxStatsEventSequence add(String name, long delta) {
    events.add(new Event(name, delta));
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    int index = 0;

    for(Event e: events) {
      sb.append("\n");
      sb.append(index);
      sb.append(" ");

      sb.append(e.name);
      sb.append(" ");

      sb.append(" +");
      if(e.delta == DELTA_DONT_CARE) {
        sb.append("DONT CARE HOW LONG HERE");
      } else {
        sb.append(e.delta);
        sb.append("ms");
      }

      sb.append(", ");

      index++;
    }

    return sb.toString();
  }

  public int countEventsOfName(String name) {
    int count = 0;

    for(Event e: events) {
      if(e.name.equals(name)) {
        count++;
      }
    }

    return count;
  }

  public MuxStatsEventSequence filterNameOut(String name) {
    MuxStatsEventSequence other = new MuxStatsEventSequence();

    long skippedDelta = 0;
    for(Event e: events) {
      if(!e.name.equals(name)) {
        other.add(e.name, e.delta + skippedDelta);
        skippedDelta = 0;
      } else {
        skippedDelta += e.delta;
      }
    }

    return other;
  }

  public ArrayList<String> validate() {
    ArrayList<String> failures = new ArrayList<>();

    if(!events.isEmpty()) {
      // If the sequence ends with viewend then find accompanying pause and end
      if (events.get(events.size() - 1).name.equals("viewend")) {
        Event a = events.get(events.size() - 3);
        Event b = events.get(events.size() - 2);
        if(!a.name.equals("pause") || !b.name.equals("ended")) {
          failures.add("Terminating viewend not preceded by necessary pause and ended events");
        }
      }

      // Error on consecutive events of the same type, except renditionchanges which are ok
      Event previous = events.get(0);

      // Ignore these events for this validation purpose
      HashSet<String> consecutiveIgnores = new HashSet<>();
      consecutiveIgnores.add("renditionchange");
      consecutiveIgnores.add("error");

      for(int i=1; i<events.size(); i++) {
        Event e = events.get(i);
        if(e.name.equals(previous.name) && !consecutiveIgnores.contains(e.name)) {
          failures.add("Repeating events of type "+previous.name+" at "+i);
        }
        previous = e;
      }

      // Ensure all rebuffer start have matching rebuffer ends
      for(int i=0; i<events.size(); i++) {
        Event e = events.get(i);
        if(e.name.equals("rebufferstart")) {
          boolean matchingFound = false;
          for(int j=i+1; !matchingFound && j<events.size(); j++) {
            Event o = events.get(j);
            if(o.name.equals("rebufferend")) {
              matchingFound = true;
            } else if(o.name.equals("rebufferstart")) {
              failures.add("Suspicious duplicate rebufferstart at "+j);
            }
          }

          if(!matchingFound) {
            failures.add("rebufferstart at "+i+" without matching end");
          }
        }
      }

      // TODO complete state transition check on MuxCore
    }

    return failures;
  }

  /**
   *
   * @param prefixMode means we compare as if a is the expected prefix of sequence b
   * @param a
   * @param b
   */
  private static void innerCompare(boolean prefixMode, MuxStatsEventSequence a, MuxStatsEventSequence b) {
    ArrayList<String> failures = new ArrayList<>();

    if(!prefixMode) {
      failures.addAll(a.validate());
    }
    failures.addAll(b.validate());

    int len = Math.min(a.events.size(), b.events.size());
    int lenMax = Math.max(a.events.size(), b.events.size());

    if(prefixMode) {
      if(len < a.events.size()) {
        failures.add("Sequence not as long as expected prefix\n");
      }
    }

    for (int i = 0; i < len; i++) {
      Event ea = a.events.get(i);
      Event eb = b.events.get(i);

      boolean eventsEqual = false;

      if (ea.name.equals(eb.name)) {
        // Allow up to 250 ms difference
        if (
            ea.delta == DELTA_DONT_CARE
                || eb.delta == DELTA_DONT_CARE
                || Math.abs(eb.delta - ea.delta) <= 250
        ) {
          eventsEqual = true;
        }
      }

      if (!eventsEqual) {
        failures.add("EV " + i + " Not equivalent " + ea + " " + eb + "\n");
      }
    }

    if(!prefixMode && len != lenMax) {
      failures.add("Event length mismatch by "+(lenMax - len)+" events\n");
    }

    if(failures.size() > 0) {
      int failureCount = failures.size();

      failures.add("a: \n"+a.toString()+"\n\n");
      failures.add("b: \n"+b.toString()+"\n\n");

      fail("Sequence comparison failed with "+failureCount+" errors:\n" + failures.toString());
    }
  }

  public static void hasPrefix(MuxStatsEventSequence prefix, MuxStatsEventSequence actual) {
    innerCompare(true, prefix, actual);
  }

  public static void compare(MuxStatsEventSequence a, MuxStatsEventSequence b) {
    innerCompare(false, a, b);
  }
}
