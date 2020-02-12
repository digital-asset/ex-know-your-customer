/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import da.refapps.knowyourcustomer.types.CddData;
import da.refapps.knowyourcustomer.types.CipData;
import da.refapps.knowyourcustomer.types.ObservationValue;
import da.refapps.knowyourcustomer.types.ScreeningData;
import da.refapps.knowyourcustomer.types.observationvalue.CDD;
import da.refapps.knowyourcustomer.types.observationvalue.CIP;
import da.refapps.knowyourcustomer.types.observationvalue.Screening;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class CsvParser {

  private static final String FIELD_SEPARATOR = ";";

  static Collection<ObservationTimeWithValue> parseData(String content, String asType) {
    final Collection<ObservationTimeWithValue> result = new ArrayList<>();
    String unescaped = content.replaceAll("\\\\n", "\n");
    try (Scanner reader = new Scanner(unescaped)) {
      while (reader.hasNextLine()) {
        String line = reader.nextLine();
        String[] timeAndValue = line.split(FIELD_SEPARATOR);
        if (timeAndValue.length < 2) {
          throw new IllegalArgumentException(
              String.format(
                  "Malformed CSV with line '%s' - it has %d fields instead of 2.",
                  line, timeAndValue.length));
        }

        Arrays.parallelSetAll(timeAndValue, (i) -> timeAndValue[i].trim());

        result.add(parseObservationTimeWithValue(asType, timeAndValue));
      }
    }
    return result;
  }

  private static ObservationTimeWithValue parseObservationTimeWithValue(
      String asType, String[] values) {
    Instant time = Instant.parse(values[0]);
    values = Arrays.copyOfRange(values, 1, values.length);
    ObservationValue value = parseObservationValue(asType, values);
    return new ObservationTimeWithValue(time, value);
  }

  private static ObservationValue parseObservationValue(String asType, String[] values) {
    switch (asType) {
      case "CDD":
        return new CDD(parseCddData(values));
      case "Screening":
        return new Screening(parseScreeningData(values));
      default:
        return new CIP(parseCipData(values));
    }
  }

  private static ScreeningData parseScreeningData(String[] values) {
    return new ScreeningData(values[0], values[1], values[2], values[3]);
  }

  private static CddData parseCddData(String[] values) {
    return new CddData(
        values[0], values[1], values[2], values[3].equals("1"), values[4], values[5], values[6]);
  }

  private static CipData parseCipData(String[] values) {
    return new CipData(values[0], values[1], values[2], values[3], values[4], values[5]);
  }
}
