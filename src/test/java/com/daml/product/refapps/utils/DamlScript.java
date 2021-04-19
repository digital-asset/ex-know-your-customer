/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.daml.product.refapps.utils;

import java.io.File;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamlScript {
  private final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

  private final ProcessBuilder processBuilder;

  private Process script;

  private DamlScript(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void run() throws Throwable {
    logger.debug("Executing: {}", String.join(" ", processBuilder.command()));
    script = processBuilder.start();
    if (script.waitFor() != 0) {
      throw new IllegalStateException("Unexpected termination of DAML script.");
    }
    logger.info("DAML Script has run successfully.");
  }

  public void kill() {
    try {
      if (script.isAlive()) {
        script.destroyForcibly().waitFor();
      }
    } catch (InterruptedException e) {
      logger.error("Could not stop DAML script.", e);
    }
  }

  public static class Builder {

    private String darPath;
    private String scriptName;
    private String sandboxPort;
    private boolean useWallclockTime = false;

    public Builder dar(Path path) {
      this.darPath = path.toString();
      return this;
    }

    public Builder scriptName(String triggerName) {
      this.scriptName = triggerName;
      return this;
    }

    public Builder sandboxPort(int port) {
      this.sandboxPort = Integer.toString(port);
      return this;
    }

    public Builder useWallclockTime() {
      this.useWallclockTime = true;
      return this;
    }

    public DamlScript build() {
      File logFile = new File(String.format("integration-test-%s.log", scriptName));
      ProcessBuilder processBuilder =
          command()
              .redirectError(ProcessBuilder.Redirect.appendTo(logFile))
              .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
      return new DamlScript(processBuilder);
    }

    private ProcessBuilder command() {
      String sandboxHost = "localhost";
      if (useWallclockTime) {
        return new ProcessBuilder()
            .command(
                "daml",
                "script",
                "--dar",
                darPath,
                "--script-name",
                scriptName,
                "--ledger-host",
                sandboxHost,
                "--ledger-port",
                sandboxPort,
                "-w");
      } else {
        return new ProcessBuilder()
            .command(
                "daml",
                "script",
                "--dar",
                darPath,
                "--script-name",
                scriptName,
                "--ledger-host",
                sandboxHost,
                "--ledger-port",
                sandboxPort);
      }
    }
  }
}
