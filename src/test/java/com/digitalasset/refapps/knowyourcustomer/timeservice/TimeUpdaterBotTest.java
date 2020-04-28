/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.timeservice;

import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.digitalasset.refapps.utils.ManualExecutorService;
import da.timeservice.timeservice.TimeManager;
import io.reactivex.Flowable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimeUpdaterBotTest extends TimeUpdaterBotBaseTest {

  public TimeUpdaterBotTest() {
    this.operator = "John Doe";
  }

  @Test
  public void newCurrentTimeIsUpdated() {
    CreatedEvent eventManager = createTimeManager();
    CreatedEvent eventCurrentTime = createCurrentTime();
    when(activeContractsClient.getActiveContracts(any(), anyBoolean()))
        .thenReturn(Flowable.just(createContractResponse(eventManager)))
        .thenReturn(Flowable.just(createContractResponse(eventCurrentTime)))
        .thenReturn(Flowable.just(createContractResponse(eventCurrentTime)));
    ManualExecutorService executor = new ManualExecutorService();

    TimeUpdaterBot bot = newBotBuilder().build();
    executor.scheduleAtFixedRate(bot::updateModelTime, 1, 1, TimeUnit.SECONDS);
    executor.runScheduledNow();

    verify(commandSubmissionClient, times(1))
        .submit(
            eq("WORKFLOW-John Doe-TimeUpdaterBot-#123"),
            eq("Test"),
            anyString(),
            eq(operator),
            commands.capture());
    List<Command> updateCommands = commands.getValue();
    TimeManager.ContractId currentTimeCid =
        new TimeManager.ContractId(eventManager.getContractId());
    ExerciseCommand expectedCommand = currentTimeCid.exerciseAdvanceCurrentTime();
    assertThat(updateCommands.size(), is(1));
    assertThat(updateCommands, everyItem(is(expectedCommand)));
  }
}
