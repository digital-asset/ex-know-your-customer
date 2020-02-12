/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.publishing;

import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.rxjava.components.LedgerViewFlowable;
import da.refapps.knowyourcustomer.types.ObservationReference;
import da.refapps.knowyourcustomer.types.ObservationValue;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface PublishingDataProvider {
  Set<Identifier> getUsedTemplates();

  Optional<ObservationValue> getObservation(
      LedgerViewFlowable.LedgerView<Template> ledgerView,
      ObservationReference reference,
      Instant time);
}
