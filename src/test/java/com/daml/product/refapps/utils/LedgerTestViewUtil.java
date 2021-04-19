/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.daml.product.refapps.utils;

import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.rxjava.components.LedgerViewFlowable;
import org.pcollections.*;

public class LedgerTestViewUtil {
  public static LedgerViewFlowable.LedgerTestView<Template> createEmptyLedgerTestView() {
    PMap<String, PMap<Identifier, PSet<String>>> stringPMapHashPMap = HashTreePMap.empty();
    PMap<Identifier, PMap<String, PSet<String>>> identifierPMapHashPMap = HashTreePMap.empty();
    PMap<Identifier, PMap<String, Template>> emptyIdMap = HashTreePMap.empty();
    return new LedgerViewFlowable.LedgerTestView<>(
        stringPMapHashPMap, identifierPMapHashPMap, emptyIdMap, emptyIdMap);
  }
}
