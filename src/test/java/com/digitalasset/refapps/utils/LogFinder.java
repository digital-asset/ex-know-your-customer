package com.digitalasset.refapps.utils;

import org.apache.commons.io.input.TailerListenerAdapter;

public class LogFinder {
  private State state = State.DISABLED;

  private enum State {DISABLED, ENABLED, FOUND}

  public boolean found() {
    return state == State.FOUND;
  }

  public void enable() {
    state = State.ENABLED;
  }

  public void process(String line) {
    if (state == State.ENABLED && matches(line)) { state = State.FOUND; }
  }

  private static boolean matches(String line) {
    return line.contains("Trigger is running");
  }
}
