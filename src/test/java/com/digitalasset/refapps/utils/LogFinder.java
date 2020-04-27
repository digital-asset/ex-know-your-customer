package com.digitalasset.refapps.utils;

public class LogFinder {
  private boolean found = false;

  public boolean found() {
    return found;
  }

  public void process(String line) {
    if (!found && matches(line)) { found = true; }
  }

  private static boolean matches(String line) {
    return line.contains("Trigger is running");
  }
}
