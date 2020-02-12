/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.timeservice;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A ScheduledThreadPoolExecutor wrapper for convenience
public class TimeUpdaterBotExecutor {
  private static final Logger logger = LoggerFactory.getLogger(TimeUpdaterBotExecutor.class);
  private final ScheduledExecutorService scheduler;

  public TimeUpdaterBotExecutor(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  public void start(TimeUpdaterBot timeUpdaterBot, Duration systemPeriodTime) {
    // Revisit this as may be unnecessary since updateModelTime tolerates race conditions
    if (systemPeriodTime.toMillis() < 100) {
      throw new IllegalArgumentException("System period time should be at least 100 ms");
    }

    TimeUnit timeUnit = TimeUnit.NANOSECONDS;
    long amount = systemPeriodTime.toNanos();
    scheduler.scheduleAtFixedRate(timeUpdaterBot::updateModelTime, amount, amount, timeUnit);
    logger.info("Started at rate {}", systemPeriodTime);
  }
}
