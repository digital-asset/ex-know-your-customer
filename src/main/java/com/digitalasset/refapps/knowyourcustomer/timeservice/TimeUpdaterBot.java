/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.timeservice;

import com.daml.ledger.javaapi.data.*;
import com.daml.ledger.javaapi.data.LedgerOffset.Absolute;
import com.daml.ledger.rxjava.LedgerClient;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder.Factory;
import com.google.common.collect.Iterables;
import da.timeservice.timeservice.CurrentTime;
import da.timeservice.timeservice.TimeManager;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeUpdaterBot {

  private static final Logger logger = LoggerFactory.getLogger(TimeUpdaterBot.class);

  private final LedgerClient client;
  private final CommandsAndPendingSetBuilder commandsAndPendingSetBuilder;
  private final FiltersByParty timeManagerFilter;
  private final FiltersByParty currentTimeFilter;

  public TimeUpdaterBot(
      LedgerClient client, Factory commandsAndPendingSetBuilderFactory, String party) {
    this(client, commandsAndPendingSetBuilderFactory, party, UUID.randomUUID().toString());
  }

  TimeUpdaterBot(
      LedgerClient client,
      CommandsAndPendingSetBuilder.Factory commandsAndPendingSetBuilderFactory,
      String party,
      String botId) {
    this.client = client;
    this.timeManagerFilter = createFilter(party, TimeManager.TEMPLATE_ID);
    this.currentTimeFilter = createFilter(party, CurrentTime.TEMPLATE_ID);
    String botName = getClass().getSimpleName();
    String workflowId = String.format("WORKFLOW-%s-%s-%s", party, botName, botId);
    this.commandsAndPendingSetBuilder =
        commandsAndPendingSetBuilderFactory.create(party, workflowId);
  }

  void updateModelTime() {
    try {
      logger.trace("Updating current time...");
      TimeManager.ContractId manager = getTimeManager();
      logger.trace("Old time: {}", getModelCurrentTime());
      submitCommand(manager.exerciseAdvanceCurrentTime());
      logger.trace("New time: {}", getModelCurrentTime());
      logger.trace("Updated current time");
    } catch (Throwable e) {
      logger.error("Exception observed during update: ", e);
      throw e;
    }
  }

  private static FiltersByParty createFilter(String party, Identifier templateId) {
    return new FiltersByParty(
        Collections.singletonMap(party, new InclusiveFilter(Collections.singleton(templateId))));
  }

  private Instant getModelCurrentTime() {
    List<CreatedEvent> createdEvents = getCreatedEvents(currentTimeFilter);
    CreatedEvent createdEvent = Iterables.getOnlyElement(createdEvents);
    CurrentTime currentTime = CurrentTime.fromValue(createdEvent.getArguments());
    return currentTime.currentTime;
  }

  private TimeManager.ContractId getTimeManager() {
    List<CreatedEvent> createdEvents = getCreatedEvents(timeManagerFilter);
    CreatedEvent createdEvent = Iterables.getOnlyElement(createdEvents);
    return new TimeManager.ContractId(createdEvent.getContractId());
  }

  private void submitCommand(Command command) {
    List<Command> commands = Collections.singletonList(command);
    SubmitCommandsRequest submitCommandsRequest =
        commandsAndPendingSetBuilder
            .newBuilder()
            .addCommand(command)
            .build()
            .get()
            .getSubmitCommandsRequest();
    client
        .getCommandSubmissionClient()
        .submit(
            submitCommandsRequest.getWorkflowId(),
            submitCommandsRequest.getApplicationId(),
            submitCommandsRequest.getCommandId(),
            submitCommandsRequest.getParty(),
            submitCommandsRequest.getLedgerEffectiveTime(),
            submitCommandsRequest.getMaximumRecordTime(),
            commands)
        .blockingGet();
  }

  private List<CreatedEvent> getCreatedEvents(FiltersByParty filter) {
    GetActiveContractsResponse activeContractSetResponse =
        client
            .getActiveContractSetClient()
            .getActiveContracts(filter, false)
            .timeout(5, TimeUnit.SECONDS)
            .blockingFirst();
    if (activeContractSetResponse.getCreatedEvents().isEmpty()) {
      Absolute offset = new Absolute(activeContractSetResponse.getOffset().get());
      Transaction transaction =
          client
              .getTransactionsClient()
              .getTransactions(offset, filter, false)
              .filter(x -> !x.getEvents().isEmpty())
              .blockingFirst();
      return transaction.getEvents().stream()
          .filter(x -> x instanceof CreatedEvent)
          .map(x -> (CreatedEvent) x)
          .collect(Collectors.toList());
    }
    return activeContractSetResponse.getCreatedEvents();
  }
}
