/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.timeservice;

import com.daml.ledger.rxjava.LedgerClient;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder.Factory;

public class TimeUpdaterBotBuilder {

  private LedgerClient client;
  private Factory commandsAndPendingSetBuilderFactory;
  private String party;
  private String botId;

  public TimeUpdaterBotBuilder setClient(LedgerClient client) {
    this.client = client;
    return this;
  }

  public TimeUpdaterBotBuilder setCommandsAndPendingSetBuilderFactory(
      Factory commandsAndPendingSetBuilderFactory) {
    this.commandsAndPendingSetBuilderFactory = commandsAndPendingSetBuilderFactory;
    return this;
  }

  public TimeUpdaterBotBuilder setParty(String party) {
    this.party = party;
    return this;
  }

  public TimeUpdaterBotBuilder setBotId(String botId) {
    this.botId = botId;
    return this;
  }

  public TimeUpdaterBot build() {
    return new TimeUpdaterBot(client, commandsAndPendingSetBuilderFactory, party, botId);
  }
}
