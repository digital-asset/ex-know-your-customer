/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.utils;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An executor that enables manual execution only and does not involve any threading. The intention
 * is to use it in tests as test double. Currently only {@link #scheduleAtFixedRate(Runnable, long,
 * long, TimeUnit)} is supported.
 *
 * <h3>Usage Example:</h3>
 *
 * <pre>{@code
 * ScheduledExecutorService dependency = new ManualExecutorService();
 * MyService sut = new MyService(dependency);
 * sut.aMethodThatCallsScheduleAtFixedRateInternally();
 * dependency.runScheduledNow();
 * }</pre>
 */
public class ManualExecutorService extends AbstractExecutorService
    implements ScheduledExecutorService {

  private Runnable command;

  public void runScheduledNow() {
    command.run();
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  /**
   * In this implementation this is almost a no-op. It merely stores the reference of the given
   * command so that it can be manually run later via {@link #runScheduledNow()}.
   *
   * @return Returns always null.
   */
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit) {
    this.command = command;
    return null;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      Runnable command, long initialDelay, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {}

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) {
    return false;
  }

  @Override
  public void execute(Runnable command) {}
}
