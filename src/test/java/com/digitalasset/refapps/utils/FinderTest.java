package com.digitalasset.refapps.utils;

import junit.framework.TestCase;

public class FinderTest extends TestCase {

  private final String alarm = "Trigger is running";
  private final String some_noise = "Some noise";

  public void testFindsMatch() {
    LogFinder finder = new LogFinder();
    finder.process(alarm);
    assertTrue(finder.found());
  }

  public void testFindsOnlyMatchingLines() {
    LogFinder finder = new LogFinder();
    finder.process(some_noise);
    assertFalse(finder.found());
  }

  public void testDoesNotFindInEmptyInput() {
    LogFinder finder = new LogFinder();
    assertFalse(finder.found());
  }

  public void testRemembersEarlierFindings() {
    LogFinder finder = new LogFinder();
    finder.process(alarm);
    finder.process(some_noise);
    assertTrue(finder.found());
  }

}
