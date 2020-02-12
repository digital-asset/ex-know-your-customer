/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import static org.junit.Assert.assertEquals;

import da.refapps.knowyourcustomer.types.CipData;
import da.refapps.knowyourcustomer.types.observationvalue.CIP;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;

public class CsvParserTest {

  private static CipData getCIPValue(ObservationTimeWithValue obs) {
    return ((CIP) obs.observationValue).cipDataValue;
  }

  @Test
  public void parserMergesDataFieldsContainingComma() {
    String csvWithNewLines =
        "2019-11-13T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 2AA;UK\n2019-11-14T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 3AA;UK\n";

    Collection<ObservationTimeWithValue> result = CsvParser.parseData(csvWithNewLines, "CIP");

    assertEquals(2, result.size());
    ArrayList<ObservationTimeWithValue> rows = new ArrayList<>(result);
    assertEquals(
        getCIPValue(rows.get(0)),
        new CipData(
            "John Smith (CEO)",
            "AA 01 23 44 B",
            "580-13-7429",
            "52/Finance and Insurance",
            "Westminster, London SW1A 2AA",
            "UK"));
    assertEquals(
        getCIPValue(rows.get(1)),
        new CipData(
            "John Smith (CEO)",
            "AA 01 23 44 B",
            "580-13-7429",
            "52/Finance and Insurance",
            "Westminster, London SW1A 3AA",
            "UK"));
  }

  @Test
  public void parserHandlesActualNewLines() {
    String csvWithNewLines =
        "2019-11-13T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 2AA;UK\n2019-11-14T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 3AA;UK\n";

    Collection<ObservationTimeWithValue> result = CsvParser.parseData(csvWithNewLines, "CIP");

    assertEquals(2, result.size());
  }

  @Test
  public void parserHandlesEscapedNewLines() {
    String csvWithEscapes =
        "2019-11-13T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 2AA;UK\\n2019-11-14T12:00:00Z;John Smith (CEO);AA 01 23 44 B;580-13-7429;52/Finance and Insurance;Westminster, London SW1A 3AA;UK\\n";

    Collection<ObservationTimeWithValue> result = CsvParser.parseData(csvWithEscapes, "CIP");

    assertEquals(2, result.size());
  }
}
