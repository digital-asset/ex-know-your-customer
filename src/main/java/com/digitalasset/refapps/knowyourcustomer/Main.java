/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer;

import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.rxjava.components.Bot;
import com.digitalasset.refapps.knowyourcustomer.publishing.CachingCsvDataProvider;
import com.digitalasset.refapps.knowyourcustomer.publishing.DataProviderBot;
import com.digitalasset.refapps.knowyourcustomer.publishing.PublishingDataProvider;
import com.digitalasset.refapps.knowyourcustomer.utils.AppParties;
import com.digitalasset.refapps.knowyourcustomer.utils.CliOptions;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Clock;
import java.time.Duration;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  // application id used for sending commands
  private static final String APPLICATION_ID = "KnowYourCustomer";
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static final Duration SYSTEM_PERIOD_TIME = Duration.ofSeconds(5);

  public static void main(String[] args) throws InterruptedException {

    CliOptions cliOptions = CliOptions.parseArgs(args);

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(cliOptions.getSandboxHost(), cliOptions.getSandboxPort())
            .usePlaintext()
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();

    DamlLedgerClient client =
        DamlLedgerClient.newBuilder(cliOptions.getSandboxHost(), cliOptions.getSandboxPort())
            .build();

    waitForSandbox(cliOptions.getSandboxHost(), cliOptions.getSandboxPort(), client);

    AppParties appParties = new AppParties(cliOptions.getParties());
    runBots(appParties, SYSTEM_PERIOD_TIME).accept(client, channel);

    logger.info("Welcome to Know Your Customer!");
    logger.info("Press Ctrl+C to shut down the program.");
    Thread.currentThread().join();
  }

  public static BiConsumer<DamlLedgerClient, ManagedChannel> runBots(
      AppParties parties, Duration systemPeriodTime) {
    return (DamlLedgerClient client, ManagedChannel channel) -> {
      Duration mrt = Duration.ofSeconds(10);
      CommandsAndPendingSetBuilder.Factory commandBuilderFactory =
          CommandsAndPendingSetBuilder.factory(APPLICATION_ID, Clock::systemUTC, mrt);

      if (parties.hasDataProvider1()) {
        logger.info("Starting automation for CIP Provider.");
        PublishingDataProvider dataProvider = new CachingCsvDataProvider("CIP");
        DataProviderBot dataProviderBot =
            new DataProviderBot(commandBuilderFactory, parties.getDataProvider1(), dataProvider);
        Bot.wire(
            APPLICATION_ID,
            client,
            dataProviderBot.getTransactionFilter(),
            dataProviderBot::calculateCommands,
            dataProviderBot::getContractInfo);
      }

      if (parties.hasDataProvider2()) {
        logger.info("Starting automation for CDD Provider.");
        PublishingDataProvider dataProvider = new CachingCsvDataProvider("CDD");
        DataProviderBot dataProviderBot =
            new DataProviderBot(commandBuilderFactory, parties.getDataProvider2(), dataProvider);
        Bot.wire(
            APPLICATION_ID,
            client,
            dataProviderBot.getTransactionFilter(),
            dataProviderBot::calculateCommands,
            dataProviderBot::getContractInfo);
      }

      if (parties.hasDataProvider3()) {
        logger.info("Starting automation for Screening provider.");
        PublishingDataProvider dataProvider = new CachingCsvDataProvider("Screening");
        DataProviderBot dataProviderBot =
            new DataProviderBot(commandBuilderFactory, parties.getDataProvider3(), dataProvider);
        Bot.wire(
            APPLICATION_ID,
            client,
            dataProviderBot.getTransactionFilter(),
            dataProviderBot::calculateCommands,
            dataProviderBot::getContractInfo);
      }
    };
  }

  private static void waitForSandbox(String host, int port, DamlLedgerClient client) {
    boolean connected = false;
    while (!connected) {
      try {
        client.connect();
        connected = true;
      } catch (Exception _ignored) {
        logger.info(String.format("Connecting to sandbox at %s:%s", host, port));
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }
}
