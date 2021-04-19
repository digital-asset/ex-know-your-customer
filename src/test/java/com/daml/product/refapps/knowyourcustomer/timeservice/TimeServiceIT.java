/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.daml.product.refapps.knowyourcustomer.timeservice;

import static com.daml.product.refapps.utils.EventuallyUtil.eventually;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.ledger.DefaultLedgerAdapter;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.javaapi.data.Party;
import com.daml.product.refapps.knowyourcustomer.triggers.TriggerTest;
import com.daml.product.refapps.utils.DamlScript;
import com.google.protobuf.InvalidProtocolBufferException;
import da.timeservice.timeservice.CurrentTime;
import da.timeservice.timeservice.TimeManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class TimeServiceIT extends TriggerTest {
  private static final Path RELATIVE_DAR_PATH =
      Paths.get("./target/know-your-customer-triggers.dar");

  private static final Party OPERATOR_PARTY = new Party("Operator");

  private static final Duration systemPeriodTime = Duration.ofMillis(100); // must be non-zero

  public static da.time.types.RelTime fromDuration(Duration modelPeriodTime) {
    long microseconds = modelPeriodTime.toMillis() * 1000;
    return new da.time.types.RelTime(microseconds);
  }

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.TimeUpdater:timeUpdaterTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return OPERATOR_PARTY;
  }

  private static final Sandbox sandbox =
      Sandbox.builder()
          .dar(RELATIVE_DAR_PATH)
          .parties(OPERATOR_PARTY.getValue())
          .useWallclockTime()
          .build();

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  private DamlScript script;
  private DefaultLedgerAdapter ledgerAdapter;
  private ContractWithId<TimeManager.ContractId> timeManager;

  @Before
  public void setUp() throws Throwable {
    // Valid port is assigned only after the sandbox has been started.
    // Therefore trigger has to be configured at the point where this can be guaranteed.
    script =
        DamlScript.builder()
            .dar(Paths.get("./target/know-your-customer.dar"))
            .scriptName("DA.RefApps.KnowYourCustomer.MarketSetupScript:setupMarketForSandbox")
            .sandboxPort(sandbox.getSandboxPort())
            .useWallclockTime()
            .build();
    script.run();
    ledgerAdapter = sandbox.getLedgerAdapter();
    timeManager = getTimeManagerCid();
  }

  @After
  public void tearDown() {
    script.kill();
  }

  private Instant getCurrentTimeInstant() {
    ContractWithId<CurrentTime.ContractId> currentTimeCid =
        ledgerAdapter.getMatchedContract(
            OPERATOR_PARTY, CurrentTime.TEMPLATE_ID, CurrentTime.ContractId::new);
    return CurrentTime.fromValue(currentTimeCid.record).currentTime;
  }

  private ContractWithId<TimeManager.ContractId> getTimeManagerCid() {
    return ledgerAdapter.getMatchedContract(
        OPERATOR_PARTY, TimeManager.TEMPLATE_ID, TimeManager.ContractId::new);
  }

  private void changeModelPeriodTime(Duration newModelPeriodTime)
      throws InvalidProtocolBufferException, InterruptedException {
    ledgerAdapter.exerciseChoice(
        OPERATOR_PARTY,
        timeManager.contractId.exerciseSetModelPeriodTime(fromDuration(newModelPeriodTime)));
  }

  private void verifyModelPeriodTime(Duration newModelPeriodTime) throws InterruptedException {
    // Passes when the difference between two subsequent times will be equal to newModelPeriodTime.
    eventually(
        () -> {
          Instant time1 = getCurrentTimeInstant();
          Instant time2 = getCurrentTimeInstant();
          assertEquals(time1.plus(newModelPeriodTime), time2);
        });
  }

  @Test
  public void modelPeriodTimeCanBeChanged()
      throws InvalidProtocolBufferException, InterruptedException {
    ledgerAdapter.exerciseChoice(OPERATOR_PARTY, timeManager.contractId.exerciseContinue());
    Duration newModelPeriodTime1 = Duration.ofHours(3);
    Duration newModelPeriodTime2 = Duration.ofHours(5);
    assertNotEquals(newModelPeriodTime1, newModelPeriodTime2);
    // The current model period is not known. To test the change two distinct values are used and
    // verified.
    changeModelPeriodTime(newModelPeriodTime1);
    verifyModelPeriodTime(newModelPeriodTime1);
    changeModelPeriodTime(newModelPeriodTime2);
    verifyModelPeriodTime(newModelPeriodTime2);
  }
}
