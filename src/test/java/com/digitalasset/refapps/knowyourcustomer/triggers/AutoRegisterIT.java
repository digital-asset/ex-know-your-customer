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
import da.refapps.knowyourcustomer.datalicense.RegisteredDataLicense;
import da.refapps.knowyourcustomer.datastream.DataStream;
import da.refapps.knowyourcustomer.datastream.EmptyDataStream;
import org.junit.*;
import org.junit.rules.ExternalResource;

public class AutoRegisterIT extends TriggerTest {

  private static final Sandbox sandbox =
      buildSandbox("DA.RefApps.KnowYourCustomer.Triggers.Tests.AutoRegisterTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return CIP_PROVIDER;
  }

  @Test
  public void testAutoRegister() {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    ContractWithId<RegisteredDataLicense.ContractId> registeredLicenseWithId =
        ledger.getMatchedContract(
            KYC_ANALYST, RegisteredDataLicense.TEMPLATE_ID, RegisteredDataLicense.ContractId::new);
    RegisteredDataLicense registeredDataLicense =
        RegisteredDataLicense.fromValue(registeredLicenseWithId.record);
    assertEquals(KYC_ANALYST.getValue(), registeredDataLicense.licenseData.consumer.party);
    assertEquals(CIP_PROVIDER.getValue(), registeredDataLicense.licenseData.publisher.party);
    assertEquals(OPERATOR.getValue(), registeredDataLicense.licenseData.operator);

    ledger.observeMatchingContracts(
        CIP_PROVIDER,
        EmptyDataStream.TEMPLATE_ID,
        EmptyDataStream::fromValue,
        true,
        this::isEmptyDataStreamWithoutConsumer,
        this::isEmptyDataStreamWithOneConsumer);

    ledger.assertDidntHappen(
        CIP_PROVIDER.getValue(),
        ContractCreated.expectContract(DataLicense.TEMPLATE_ID, "{IGNORE}"));
    ledger.assertDidntHappen(
        KYC_ANALYST.getValue(),
        ContractCreated.expectContract(RegisteredDataLicense.TEMPLATE_ID, "{IGNORE}"));
    ledger.assertDidntHappen(
        CIP_PROVIDER.getValue(),
        ContractCreated.expectContract(DataStream.TEMPLATE_ID, "{IGNORE}"));
  }

  private boolean isEmptyDataStreamWithoutConsumer(EmptyDataStream emptyDataStream) {
    return hasRightStakeholders(emptyDataStream) && emptyDataStream.consumers.isEmpty();
  }

  private boolean isEmptyDataStreamWithOneConsumer(EmptyDataStream emptyDataStream) {
    return hasRightStakeholders(emptyDataStream)
        && emptyDataStream.consumers.size() == 1
        && emptyDataStream.consumers.get(0).party.equals(KYC_ANALYST.getValue());
  }

  private boolean hasRightStakeholders(EmptyDataStream emptyDataStream) {
    return CIP_PROVIDER.getValue().equals(emptyDataStream.publisher.party)
        && OPERATOR.getValue().equals(emptyDataStream.operator);
  }
}
