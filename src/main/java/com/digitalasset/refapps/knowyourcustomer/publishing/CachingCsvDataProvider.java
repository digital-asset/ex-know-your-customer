/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import static com.digitalasset.refapps.knowyourcustomer.utils.BotUtil.filterTemplates;

import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.rxjava.components.LedgerViewFlowable.LedgerView;
import com.google.common.collect.Sets;
import da.refapps.knowyourcustomer.datasource.DataSource;
import da.refapps.knowyourcustomer.types.ObservationReference;
import da.refapps.knowyourcustomer.types.ObservationValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class CachingCsvDataProvider implements PublishingDataProvider {

  private static final String DATA_DIR = "data";
  private final ConcurrentHashMap<
          ObservationReference, ConcurrentLinkedQueue<ObservationTimeWithValue>>
      cache = new ConcurrentHashMap<>();
  private final Function<String, String> readFile;
  private final String observationReferenceType;

  public CachingCsvDataProvider(String asType) {
    this.readFile = CachingCsvDataProvider::readFileFromDataDir;
    this.observationReferenceType = asType;
  }

  public CachingCsvDataProvider(Function<String, String> readFile) {
    this.readFile = readFile;
    this.observationReferenceType = "CIP";
  }

  @Override
  public Set<Identifier> getUsedTemplates() {
    return Sets.newHashSet(DataSource.TEMPLATE_ID);
  }

  public Optional<ObservationValue> getObservation(
      LedgerView<Template> ledgerView, ObservationReference reference, Instant time) {
    if (!cache.containsKey(reference)) {
      initCache(ledgerView);
    }
    ConcurrentLinkedQueue<ObservationTimeWithValue> dataForReference =
        cache.getOrDefault(reference, new ConcurrentLinkedQueue<>());
    return selectDataInActualTimeWindow(dataForReference, time);
  }

  public static String readFileFromDataDir(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(DATA_DIR, path)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void initCache(LedgerView<Template> ledgerView) {
    Collection<DataSource> dataSources = getDataSources(ledgerView);
    for (DataSource dataSource : dataSources) {
      cache.computeIfAbsent(dataSource.reference, x -> parseData(dataSource));
    }
  }

  private ConcurrentLinkedQueue<ObservationTimeWithValue> parseData(DataSource dataSource) {
    return new ConcurrentLinkedQueue<>(
        CsvParser.parseData(readFile.apply(dataSource.path), observationReferenceType));
  }

  private Collection<DataSource> getDataSources(LedgerView<Template> ledgerView) {
    return filterTemplates(DataSource.class, ledgerView.getContracts(DataSource.TEMPLATE_ID))
        .values();
  }

  private Optional<ObservationValue> selectDataInActualTimeWindow(
      ConcurrentLinkedQueue<ObservationTimeWithValue> data, Instant time) {
    ObservationTimeWithValue nextValue = data.peek();
    ObservationValue result = null;
    while (nextValue != null && !nextValue.time.isAfter(time)) {
      result = data.poll().observationValue;
      nextValue = data.peek();
    }

    return Optional.ofNullable(result);
  }
}
