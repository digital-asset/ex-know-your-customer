/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.utils;

public class LogFinder {
  private volatile boolean found = false;

  public boolean found() {
    return found;
  }

  public void process(String line) {
    if (!found && matches(line)) {
      found = true;
    }
  }

  private static boolean matches(String line) {
    return line.replaceAll(" ", "").contains("Trigger" + "is" + "running");
  }
}
