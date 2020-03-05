/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import static com.digitalasset.refapps.knowyourcustomer.utils.BotUtil.filterTemplates;

import com.daml.ledger.javaapi.data.*;
import com.daml.ledger.rxjava.components.LedgerViewFlowable;
import com.daml.ledger.rxjava.components.helpers.CommandsAndPendingSet;
import com.daml.ledger.rxjava.components.helpers.CreatedContract;
import com.daml.ledger.rxjava.components.helpers.TemplateUtils;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder;
import com.digitalasset.refapps.knowyourcustomer.utils.CommandsAndPendingSetBuilder.Factory;
import com.google.common.collect.Sets;
import da.refapps.knowyourcustomer.datasource.DataSource;
import da.refapps.knowyourcustomer.datastream.DataStream;
import da.refapps.knowyourcustomer.types.ObservationValue;
import da.timeservice.timeservice.CurrentTime;
import io.reactivex.Flowable;
import java.time.Instant;
import java.util.*;

/** An automation bot that publishes values on streams given by a data provider. */
public class DataProviderBot {

  private final CommandsAndPendingSetBuilder commandsAndPendingSetBuilder;
  private final TransactionFilter transactionFilter;
  private final String partyName;
  private final PublishingDataProvider publishingDataProvider;

  public DataProviderBot(
      Factory commandsAndPendingSetBuilderFactory,
      String partyName,
      PublishingDataProvider publishingDataProvider) {
    this.partyName = partyName;
    this.publishingDataProvider = publishingDataProvider;
    String workflowId =
        "WORKFLOW-" + partyName + "-DataProviderBot-" + UUID.randomUUID().toString();
    commandsAndPendingSetBuilder =
        commandsAndPendingSetBuilderFactory.create(partyName, workflowId);

    Set<Identifier> templateSet =
        Sets.union(
            Sets.newHashSet(DataStream.TEMPLATE_ID, CurrentTime.TEMPLATE_ID),
            publishingDataProvider.getUsedTemplates());
    Filter streamFilter = new InclusiveFilter(templateSet);
    transactionFilter = new FiltersByParty(Collections.singletonMap(partyName, streamFilter));
  }

  public Flowable<CommandsAndPendingSet> calculateCommands(
      LedgerViewFlowable.LedgerView<Template> ledgerView) {
    CommandsAndPendingSetBuilder.Builder builder = commandsAndPendingSetBuilder.newBuilder();

    getCurrentTime(ledgerView)
        .ifPresent(currentTime -> updateAllDataStreams(ledgerView, currentTime, builder));

    return builder.buildFlowable();
  }

  public TransactionFilter getTransactionFilter() {
    return transactionFilter;
  }

  public Template getContractInfo(CreatedContract createdContract) {
    //noinspection unchecked
    return TemplateUtils.contractTransformer(DataStream.class, CurrentTime.class, DataSource.class)
        .apply(createdContract);
  }

  private void updateAllDataStreams(
      LedgerViewFlowable.LedgerView<Template> ledgerView,
      Instant currentTime,
      CommandsAndPendingSetBuilder.Builder cmdBuilder) {
    Map<String, DataStream> dataStreams =
        filterTemplates(DataStream.class, ledgerView.getContracts(DataStream.TEMPLATE_ID));

    for (Map.Entry<String, DataStream> dsWithCid : dataStreams.entrySet()) {
      DataStream dataStream = dsWithCid.getValue();
      if (dataStream.publisher.party.equals(partyName)
          && currentTime.isAfter(dataStream.observation.time)) {
        Optional<ObservationValue> optionalObservation =
            publishingDataProvider.getObservation(
                ledgerView, dataStream.observation.label, currentTime);
        final DataStream.ContractId dataStreamCid = new DataStream.ContractId(dsWithCid.getKey());
        if (optionalObservation.isPresent()) {
          cmdBuilder.addCommand(
              dataStreamCid.exerciseUpdateObservation(currentTime, optionalObservation.get()));
        } else if (!dataStream.lastUpdated.equals(currentTime)) {
          cmdBuilder.addCommand(dataStreamCid.exerciseUpdateLicenses());
        }
      }
    }
  }

  private Optional<Instant> getCurrentTime(LedgerViewFlowable.LedgerView<Template> ledgerView) {
    Map<String, CurrentTime> currentTimeContracts =
        filterTemplates(CurrentTime.class, ledgerView.getContracts(CurrentTime.TEMPLATE_ID));
    return currentTimeContracts.values().stream()
        .findFirst()
        .map(currentTime -> currentTime.currentTime);
  }
}
