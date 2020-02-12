/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.extensions;

import java.time.Duration;

public class RelTime {
  public static da.time.types.RelTime fromDuration(Duration modelPeriodTime) {
    long microseconds = modelPeriodTime.toMillis() * 1000;
    return new da.time.types.RelTime(microseconds);
  }
}
