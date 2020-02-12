/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AppParties {
  private static final String DATA_PROVIDER_1 = "CIP_Provider";
  private static final String DATA_PROVIDER_2 = "CDD_Provider";
  private static final String DATA_PROVIDER_3 = "ScreeningProvider";

  private static final String OPERATOR = "Operator";

  public static final String[] ALL_PARTIES =
      new String[] {DATA_PROVIDER_1, DATA_PROVIDER_2, DATA_PROVIDER_3, OPERATOR};
  private final Set<String> parties;

  public AppParties(String[] parties) {
    this.parties = new HashSet<>(Arrays.asList(parties));
  }

  public boolean hasDataProvider1() {
    return parties.contains(DATA_PROVIDER_1);
  }

  public String getDataProvider1() {
    return DATA_PROVIDER_1;
  }

  public boolean hasDataProvider2() {
    return parties.contains(DATA_PROVIDER_2);
  }

  public String getDataProvider2() {
    return DATA_PROVIDER_2;
  }

  public boolean hasDataProvider3() {
    return parties.contains(DATA_PROVIDER_3);
  }

  public String getDataProvider3() {
    return DATA_PROVIDER_3;
  }

  public boolean hasOperator() {
    return parties.contains(OPERATOR);
  }

  public String getOperator() {
    return OPERATOR;
  }
}
