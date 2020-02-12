/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.triggers;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.refapps.utils.Trigger;
import com.digitalasset.testing.junit4.Sandbox;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.*;

public abstract class TriggerTest {
  protected static final Party OPERATOR = new Party("Operator");
  protected static final Party CIP_PROVIDER = new Party("CIP_Provider");
  protected static final Party BANK_1 = new Party("Bank1");
  protected static final Party KYC_ANALYST = new Party("KYC_Analyst");
  protected static final Party KYC_REVIEWER = new Party("KYC_Reviewer");
  protected static final Party KYC_QUALITYASSURANCE = new Party("KYC_QA");

  protected static Sandbox buildSandbox(String module) {
    return Sandbox.builder()
        .dar(Paths.get("target/know-your-customer.dar"))
        .parties(OPERATOR, CIP_PROVIDER, BANK_1, KYC_ANALYST, KYC_REVIEWER, KYC_QUALITYASSURANCE)
        .module(module)
        .scenario("setup")
        .timeout(Duration.ofSeconds(90))
        .build();
  }

  protected abstract int getSandboxPort();

  protected abstract String getTriggerName();

  protected abstract Party getTriggerParty();

  private Trigger testedTrigger;

  @Before
  public void setup() throws Throwable {
    // Valid port is assigned only after the sandbox has been started.
    // Therefore trigger has to be configured at the point where this can be guaranteed.
    testedTrigger =
        Trigger.builder()
            .dar(Paths.get("./target/know-your-customer.dar"))
            .triggerName(getTriggerName())
            .sandboxPort(getSandboxPort())
            .party(getTriggerParty())
            .build();
    testedTrigger.start();
  }

  @After
  public void tearDown() {
    testedTrigger.stop();
  }
}
