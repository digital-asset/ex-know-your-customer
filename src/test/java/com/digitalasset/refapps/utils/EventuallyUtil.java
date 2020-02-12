/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.utils;

import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class EventuallyUtil {
  public static void eventually(Runnable code) throws InterruptedException {
    eventually(
        () -> {
          code.run();
          return null;
        });
  }

  public static <T> T eventually(Supplier<T> code) throws InterruptedException {
    Instant started = Instant.now();
    boolean finished = false;
    T result = null;
    while (!finished) {
      try {
        result = code.get();
        finished = true;
      } catch (Throwable t) {
        if (Duration.between(started, Instant.now()).compareTo(Duration.ofSeconds(60)) > 0) {
          fail("Code did not succeed within timeout.");
        } else {
          Thread.sleep(200);
          finished = false;
        }
      }
    }
    return result;
  }
}
