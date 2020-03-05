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
import da.refapps.knowyourcustomer.datalicense.RegisteredDataLicense;
import da.refapps.knowyourcustomer.datastream.DataStream;
import da.refapps.knowyourcustomer.kycextension.ResearchProcess;
import java.io.IOException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class AutoStartResearchProcessIT extends TriggerTest {

  private static final Sandbox sandbox =
      buildSandbox("Test.DA.RefApps.KnowYourCustomer.Triggers.AutoStartResearchTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.AutoStartResearch:autoStartResearchProcessTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return KYC_ANALYST;
  }

  @Test
  public void testAutoStartResearchWithRegister() throws IOException {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    ContractWithId<RegisteredDataLicense.ContractId> registeredLicenseWithId =
        ledger.getMatchedContract(
            BANK_1, RegisteredDataLicense.TEMPLATE_ID, RegisteredDataLicense.ContractId::new);
    RegisteredDataLicense registeredDataLicense =
        RegisteredDataLicense.fromValue(registeredLicenseWithId.record);
    assertEquals(BANK_1.getValue(), registeredDataLicense.licenseData.consumer.party);
    assertEquals(KYC_ANALYST.getValue(), registeredDataLicense.licenseData.publisher.party);
    assertEquals(OPERATOR.getValue(), registeredDataLicense.licenseData.operator);

    ledger.observeMatchingContracts(
        KYC_ANALYST,
        DataStream.TEMPLATE_ID,
        DataStream::fromValue,
        true,
        this::isDataStreamWithoutConsumer,
        this::isDataStreamWithOneConsumer);

    ContractWithId<ResearchProcess.ContractId> researchProcessWithId =
        ledger.getMatchedContract(
            KYC_ANALYST, ResearchProcess.TEMPLATE_ID, ResearchProcess.ContractId::new);
    ResearchProcess researchProcess = ResearchProcess.fromValue(researchProcessWithId.record);
    assertEquals(KYC_ANALYST.getValue(), researchProcess.analyst);
    assertEquals(KYC_REVIEWER.getValue(), researchProcess.reviewer);
    assertEquals(KYC_QUALITYASSURANCE.getValue(), researchProcess.qualityAssurance);

    ledger.assertDidntHappen(
        BANK_1.getValue(),
        ContractCreated.expectContract(RegisteredDataLicense.TEMPLATE_ID, "{IGNORE}"));
    ledger.assertDidntHappen(
        KYC_ANALYST.getValue(), ContractCreated.expectContract(DataStream.TEMPLATE_ID, "{IGNORE}"));
  }

  private boolean isDataStreamWithoutConsumer(DataStream dataStream) {
    return hasRightStakeholders(dataStream) && dataStream.consumers.isEmpty();
  }

  private boolean isDataStreamWithOneConsumer(DataStream dataStream) {
    return hasRightStakeholders(dataStream)
        && dataStream.consumers.size() == 1
        && dataStream.consumers.get(0).party.equals(BANK_1.getValue());
  }

  private boolean hasRightStakeholders(DataStream dataStream) {
    return KYC_ANALYST.getValue().equals(dataStream.publisher.party)
        && OPERATOR.getValue().equals(dataStream.operator);
  }
}
