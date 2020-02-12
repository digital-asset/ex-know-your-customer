/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.timeservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder.Factory;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TimeUpdaterBotExecutorTest {

  @Test(expected = IllegalArgumentException.class)
  public void cannotStartWithTooShortSystemPeriodTime() {
    TimeUpdaterBotExecutor botExecutor = new TimeUpdaterBotExecutor(null);
    botExecutor.start(null, Duration.ofMillis(100 - 1));
  }

  @Test
  public void startSchedulesBotAtFixedRate() {
    ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
    Factory commandsFactory =
        CommandsAndPendingSetBuilder.factory("UnitTest", Clock::systemUTC, Duration.ZERO);
    TimeUpdaterBot dummyBot = new TimeUpdaterBot(null, commandsFactory, null);

    TimeUpdaterBotExecutor botExecutor = new TimeUpdaterBotExecutor(executorService);
    botExecutor.start(dummyBot, Duration.ofMillis(142));

    verify(executorService)
        .scheduleAtFixedRate(
            any(Runnable.class), eq(142_000_000L), eq(142_000_000L), eq(TimeUnit.NANOSECONDS));
  }
}
