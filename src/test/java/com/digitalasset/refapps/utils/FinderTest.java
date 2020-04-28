/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.utils;

import junit.framework.TestCase;

public class FinderTest extends TestCase {

  private final String ALARM = "Trigger is running";
  private final String NOISE = "Some noise";

  public void testFindsMatch() {
    LogFinder finder = new LogFinder();
    finder.process(ALARM);
    assertTrue(finder.found());
  }

  public void testFindsOnlyMatchingLines() {
    LogFinder finder = new LogFinder();
    finder.process(NOISE);
    assertFalse(finder.found());
  }

  public void testDoesNotFindInEmptyInput() {
    LogFinder finder = new LogFinder();
    assertFalse(finder.found());
  }

  public void testRemembersEarlierFindings() {
    LogFinder finder = new LogFinder();
    finder.process(ALARM);
    finder.process(NOISE);
    assertTrue(finder.found());
  }
}
