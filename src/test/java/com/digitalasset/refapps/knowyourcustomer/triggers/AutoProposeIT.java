/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.triggers;

import static org.junit.Assert.assertEquals;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;
import da.refapps.knowyourcustomer.roles.DataLicenseProposal;
import org.junit.*;
import org.junit.rules.ExternalResource;

public class AutoProposeIT extends TriggerTest {
  private static final Sandbox sandbox =
      buildSandbox("DA.RefApps.KnowYourCustomer.Triggers.Tests.AutoProposeTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return CIP_PROVIDER;
  }

  @Ignore
  @Test
  public void testAutoProposal() {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    ContractWithId<DataLicenseProposal.ContractId> proposalWithId =
        ledger.getMatchedContract(
            KYC_ANALYST, DataLicenseProposal.TEMPLATE_ID, DataLicenseProposal.ContractId::new);

    DataLicenseProposal proposal = DataLicenseProposal.fromValue(proposalWithId.record);

    assertEquals(KYC_ANALYST.getValue(), proposal.consumer.party);
    assertEquals(CIP_PROVIDER.getValue(), proposal.publisher.party);
    assertEquals(OPERATOR.getValue(), proposal.operator);

    ledger.assertDidntHappen(
        KYC_ANALYST.getValue(),
        ContractCreated.expectContract(DataLicenseProposal.TEMPLATE_ID, "{IGNORE}"));
  }
}
