/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.utils;

import static com.digitalasset.refapps.utils.EventuallyUtil.eventually;

import com.daml.ledger.javaapi.data.Party;
import java.nio.file.Path;
import java.util.Scanner;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trigger {
  private final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

  private final ProcessBuilder processBuilder;

  private Process trigger;

  private Trigger(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void start() throws Throwable {
    logger.debug("Executing: {}", String.join(" ", processBuilder.command()));
    trigger = processBuilder.start();
    try (Scanner scanner = new Scanner(trigger.getInputStream())) {
      eventually(() -> Assert.assertTrue(scanner.nextLine().contains("Trigger is running")));
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
      ProcessBuilder processBuilder = command().redirectErrorStream(true);
      return new Trigger(processBuilder);
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
