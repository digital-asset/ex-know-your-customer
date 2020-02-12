/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import da.refapps.knowyourcustomer.types.ObservationValue;
import java.time.Instant;
import java.util.Objects;

class ObservationTimeWithValue {
  final Instant time;
  final ObservationValue observationValue;

  ObservationTimeWithValue(Instant time, ObservationValue observationValue) {
    this.time = time;
    this.observationValue = observationValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ObservationTimeWithValue that = (ObservationTimeWithValue) o;
    return observationValue.equals(that.observationValue) && time.equals(that.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(observationValue, time);
  }
}
