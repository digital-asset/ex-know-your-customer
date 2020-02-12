/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import static com.digitalasset.refapps.knowyourcustomer.assertions.Assert.assertEmpty;
import static com.digitalasset.refapps.knowyourcustomer.assertions.Assert.assertOptionalValue;
import static com.digitalasset.refapps.utils.LedgerTestViewUtil.createEmptyLedgerTestView;

import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.rxjava.components.LedgerViewFlowable;
import com.daml.ledger.rxjava.components.LedgerViewFlowable.LedgerTestView;
import da.refapps.knowyourcustomer.datasource.DataSource;
import da.refapps.knowyourcustomer.types.CipData;
import da.refapps.knowyourcustomer.types.ObservationReference;
import da.refapps.knowyourcustomer.types.ObservationValue;
import da.refapps.knowyourcustomer.types.observationvalue.CIP;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;

public class CachingCsvDataProviderTest {
  private static final String someParty = "party1";
  private static final ObservationReference reference =
      new ObservationReference("ACME", true, true, true);
  private static final String content =
      String.join(
          "\\n",
          "2019-11-12T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 1AA;UK",
          "2019-11-13T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 2AA;UK",
          "2019-11-14T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 3AA;UK",
          "2019-11-15T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 4AA;UK");
  private static String path = "path";
  private static final DataSource dataSource =
      new DataSource(someParty, someParty, Collections.emptyList(), reference, path);
  private static final LedgerViewFlowable.LedgerView<Template> ledgerView =
      createEmptyLedgerTestView().addActiveContract(DataSource.TEMPLATE_ID, "cid1", dataSource);

  @Test
  public void correctlySpecifiedTimeHasOneObservationValue() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> content);
    Instant currentTime = Instant.parse("2019-11-12T12:00:00Z");

    Optional<ObservationValue> result = sut.getObservation(ledgerView, reference, currentTime);
    assertOptionalValue(cip("1"), result);
  }

  @Test
  public void exactTimeHasOneObservationValue() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> content);
    Instant currentTime = Instant.parse("2019-11-12T12:00:00Z");

    Optional<ObservationValue> result = sut.getObservation(ledgerView, reference, currentTime);
    assertOptionalValue(cip("1"), result);
  }

  @Test
  public void tooEarlyCurrentTimeHasNoObservationValue() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> content);
    Instant currentTime = Instant.parse("2007-12-01T11:00:30.00Z");

    Optional<ObservationValue> result = sut.getObservation(ledgerView, reference, currentTime);
    assertEmpty(result);
  }

  @Test
  public void continuousConsumptionAlwaysYieldsOneObservationValue() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> content);
    Instant currentTime = Instant.parse("2019-11-12T12:00:00Z");

    Optional<ObservationValue> result = sut.getObservation(ledgerView, reference, currentTime);
    assertOptionalValue(cip("1"), result);

    currentTime = Instant.parse("2019-11-13T12:00:00Z");

    result = sut.getObservation(ledgerView, reference, currentTime);
    assertOptionalValue(cip("2"), result);

    currentTime = Instant.parse("2019-11-14T12:00:00Z");

    result = sut.getObservation(ledgerView, reference, currentTime);
    assertOptionalValue(cip("3"), result);

    currentTime = Instant.parse("2019-11-15T12:00:00Z");

    result = sut.getObservation(ledgerView, reference, currentTime);
    assertOptionalValue(cip("4"), result);
  }

  @Test
  public void latestObservationValueIsReturned() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> content);
    Instant currentTime = Instant.parse("2019-11-12T12:00:00Z");

    Optional<ObservationValue> result = sut.getObservation(ledgerView, reference, currentTime);

    assertOptionalValue(cip("1"), result);
  }

  @Test
  public void noObservationValueWhenContentIsEmpty() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> "");
    ObservationReference reference = new ObservationReference("Foo", true, true, true);
    DataSource emptyDataSource =
        new DataSource(someParty, someParty, Collections.emptyList(), reference, "empty-path");
    LedgerTestView<Template> ledgerView =
        createEmptyLedgerTestView()
            .addActiveContract(DataSource.TEMPLATE_ID, "emptyCid", emptyDataSource);
    Instant currentTime = Instant.parse("2007-12-04T11:00:30.00Z");

    Optional<ObservationValue> result = sut.getObservation(ledgerView, reference, currentTime);

    assertEmpty(result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void exceptionIsThrownWhenCsvCannotBeParsed() {
    PublishingDataProvider sut = new CachingCsvDataProvider(path -> "gibberish");
    ObservationReference reference = new ObservationReference("Foo", true, true, true);
    DataSource badDataSource =
        new DataSource(someParty, someParty, Collections.emptyList(), reference, "gibberish-path");
    LedgerTestView<Template> ledgerView =
        createEmptyLedgerTestView()
            .addActiveContract(DataSource.TEMPLATE_ID, "badCid", badDataSource);
    Instant currentTime = Instant.parse("2007-12-04T11:00:30.00Z");

    sut.getObservation(ledgerView, reference, currentTime);
  }

  private CIP cip(String i) {
    return new CIP(
        new CipData(
            "John Smith (CEO)",
            "AA 01 23 44 B",
            "580-13-7429",
            "52/Finance and Insurance",
            "Westminster, London SW1A " + i + "AA",
            "UK"));
  }
}
