/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.assertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.rxjava.components.helpers.CommandsAndPendingSet;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import java.util.List;
import java.util.Optional;

public class Assert {

  public static void assertHasSingleExercise(
      CommandsAndPendingSet cmds, String cid, String choiceName) {
    List<Command> actualCommands = cmds.getSubmitCommandsRequest().getCommands();
    assertEquals(1, actualCommands.size());
    actualCommands.forEach(
        cmd -> {
          Optional<ExerciseCommand> exerciseCommand = cmd.asExerciseCommand();
          assertTrue(exerciseCommand.isPresent());
          assertEquals(cid, exerciseCommand.get().getContractId());
          assertEquals(choiceName, exerciseCommand.get().getChoice());
        });
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <T> void assertOptionalValue(T expectedValue, Optional<T> actual) {
    assertTrue(actual.isPresent());
    assertEquals(expectedValue, actual.get());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <T> void assertEmpty(Optional<T> result) {
    assertFalse(result.isPresent());
  }

  public static void assertContractsCreatedExactlyNTimes(
      Party party, DefaultLedgerAdapter ledger, int n, Identifier tid) {
    for (int i = 0; i < n; i++) {
      ledger.getMatchedContract(party, tid, cid -> null);
    }
    ledger.assertDidntHappen(party.getValue(), ContractCreated.expectContract(tid, "{IGNORE}"));
  }
}
