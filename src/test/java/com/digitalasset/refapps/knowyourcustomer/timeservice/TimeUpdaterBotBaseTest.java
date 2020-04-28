/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.timeservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.GetActiveContractsResponse;
import com.daml.ledger.rxjava.ActiveContractsClient;
import com.daml.ledger.rxjava.CommandSubmissionClient;
import com.daml.ledger.rxjava.LedgerClient;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder.Factory;
import com.google.protobuf.Empty;
import da.timeservice.timeservice.CurrentTime;
import da.timeservice.timeservice.TimeManager;
import io.reactivex.Single;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public abstract class TimeUpdaterBotBaseTest {

  @Mock protected LedgerClient ledgerClient;
  @Mock protected CommandSubmissionClient commandSubmissionClient;
  @Mock protected ActiveContractsClient activeContractsClient;
  @Captor protected ArgumentCaptor<List<Command>> commands;

  String operator = "Dummy Operator";

  @Before
  public void setup() {
    when(ledgerClient.getCommandSubmissionClient()).thenReturn(commandSubmissionClient);
    when(commandSubmissionClient.submit(any(), any(), any(), any(), any()))
        .thenReturn(Single.just(Empty.newBuilder().build()));
    when(ledgerClient.getActiveContractSetClient()).thenReturn(activeContractsClient);
  }

  CreatedEvent createTimeManager() {
    TimeManager timeManager = new TimeManager(operator);
    return new CreatedEvent(
        Collections.singletonList(operator),
        "#0:0",
        TimeManager.TEMPLATE_ID,
        "123",
        timeManager.toValue(),
        Optional.empty(),
        Optional.empty(),
        Collections.singleton(operator),
        Collections.emptyList());
  }

  CreatedEvent createCurrentTime() {
    CurrentTime currentTime = new CurrentTime(operator, Instant.MIN, Collections.emptyList());
    return new CreatedEvent(
        Collections.singletonList(operator),
        "#0:0",
        CurrentTime.TEMPLATE_ID,
        "123",
        currentTime.toValue(),
        Optional.empty(),
        Optional.empty(),
        Collections.singleton(operator),
        Collections.emptyList());
  }

  TimeUpdaterBotBuilder newBotBuilder() {
    Factory commandsAndPendingSetBuilderFactory =
        CommandsAndPendingSetBuilder.factory("Test", Clock::systemUTC, Duration.ofMillis(42));
    String botId = "#123";
    return new TimeUpdaterBotBuilder()
        .setClient(ledgerClient)
        .setCommandsAndPendingSetBuilderFactory(commandsAndPendingSetBuilderFactory)
        .setParty(operator)
        .setBotId(botId);
  }

  GetActiveContractsResponse createContractResponse(CreatedEvent event) {
    return new GetActiveContractsResponse(null, Collections.singletonList(event), null);
  }
}
