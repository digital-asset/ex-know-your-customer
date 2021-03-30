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
import da.refapps.knowyourcustomer.datalicense.DataLicense;
import org.junit.*;
import org.junit.rules.ExternalResource;

public class AutoAcceptIT extends TriggerTest {

  private static final Sandbox sandbox =
      buildSandbox("DA.RefApps.KnowYourCustomer.Triggers.Tests.AutoAcceptTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoAcceptTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return KYC_ANALYST;
  }

  @Ignore
  @Test
  public void testAutoAccept() {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    ContractWithId<DataLicense.ContractId> licenseWithId =
        ledger.getMatchedContract(
            KYC_ANALYST, DataLicense.TEMPLATE_ID, DataLicense.ContractId::new);
    DataLicense license = DataLicense.fromValue(licenseWithId.record);

    assertEquals(KYC_ANALYST.getValue(), license.licenseData.consumer.party);
    assertEquals(CIP_PROVIDER.getValue(), license.licenseData.publisher.party);
    assertEquals(OPERATOR.getValue(), license.licenseData.operator);

    ledger.assertDidntHappen(
        KYC_ANALYST.getValue(),
        ContractCreated.expectContract(DataLicense.TEMPLATE_ID, "{IGNORE}"));
  }
}
