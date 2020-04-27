/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.utils;

import static com.digitalasset.refapps.utils.EventuallyUtil.eventually;

import com.daml.ledger.javaapi.data.Party;
import java.io.File;
import java.nio.file.Path;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trigger {
  private final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

  private final ProcessBuilder processBuilder;
  private final File logFile;

  private Process trigger;

  private Trigger(ProcessBuilder processBuilder, File logFile) {
    this.processBuilder = processBuilder;
    this.logFile = logFile;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void start() throws Throwable {
    logger.debug("Executing: {}", String.join(" ", processBuilder.command()));

    class TriggerTailerListener extends TailerListenerAdapter {
      private LogFinder logFinder;

      TriggerTailerListener(LogFinder logFinder) {
        this.logFinder = logFinder;
      }

      public void handle(String line) {
        logFinder.process(line);
      }
    }

    LogFinder logFinder = new LogFinder();
    TriggerTailerListener listener = new TriggerTailerListener(logFinder);
    boolean fromEndOfFile = true;
    int delayMillis = 1000;
    Tailer tailer = new Tailer(logFile, listener, delayMillis, fromEndOfFile);
    Thread thread = new Thread(tailer);
    thread.start();

    trigger = processBuilder.start();
    try {
      eventually(() -> Assert.assertTrue(logFinder.found()));
    } finally{
      tailer.stop();
    }
    logger.info("Started.");
  }

  public void stop() {
    try {
      trigger.destroyForcibly().waitFor();
    } catch (InterruptedException e) {
      logger.error("Could not stop trigger.", e);
    }
  }

  public static class Builder {

    private String darPath;
    private String triggerName;
    private String sandboxPort;
    private String party;

    public Builder dar(Path path) {
      this.darPath = path.toString();
      return this;
    }

    public Builder triggerName(String triggerName) {
      this.triggerName = triggerName;
      return this;
    }

    public Builder sandboxPort(int port) {
      this.sandboxPort = Integer.toString(port);
      return this;
    }

    public Builder party(Party party) {
      this.party = party.getValue();
      return this;
    }

    public Trigger build() {
      File logFile = new File(String.format("integration-test-%s.log", triggerName));
      ProcessBuilder processBuilder =
          command()
              .redirectError(ProcessBuilder.Redirect.appendTo(logFile))
              .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
      return new Trigger(processBuilder, logFile);
    }

    private ProcessBuilder command() {
      String sandboxHost = "localhost";
      return new ProcessBuilder()
          .command(
              "daml",
              "trigger",
              "--static-time",
              "--dar",
              darPath,
              "--trigger-name",
              triggerName,
              "--ledger-host",
              sandboxHost,
              "--ledger-port",
              sandboxPort,
              "--ledger-party",
              party);
    }
  }
}
