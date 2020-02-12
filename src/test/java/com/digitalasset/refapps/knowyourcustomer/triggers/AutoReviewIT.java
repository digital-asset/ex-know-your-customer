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
import da.refapps.knowyourcustomer.kycextension.QualityAssuranceRequest;
import da.refapps.knowyourcustomer.kycextension.ReviewRequest;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class AutoReviewIT extends TriggerTest {
  private static final Sandbox sandbox =
      buildSandbox("Test.DA.RefApps.KnowYourCustomer.Triggers.AutoReviewTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoReviewTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return KYC_REVIEWER;
  }

  @Test
  public void testAutoReview() {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    ledger.getMatchedContract(
        KYC_REVIEWER, ReviewRequest.TEMPLATE_ID, ReviewRequest.ContractId::new);

    ContractWithId<QualityAssuranceRequest.ContractId> qualityAssuranceReqWithId =
        ledger.getMatchedContract(
            KYC_REVIEWER,
            QualityAssuranceRequest.TEMPLATE_ID,
            QualityAssuranceRequest.ContractId::new);

    QualityAssuranceRequest qaRequest =
        QualityAssuranceRequest.fromValue(qualityAssuranceReqWithId.record);

    assertEquals(KYC_ANALYST.getValue(), qaRequest.analyst);
    assertEquals(KYC_REVIEWER.getValue(), qaRequest.reviewer);
    assertEquals(KYC_QUALITYASSURANCE.getValue(), qaRequest.qualityAssurance);

    ledger.assertDidntHappen(
        KYC_REVIEWER.getValue(),
        ContractCreated.expectContract(QualityAssuranceRequest.TEMPLATE_ID, "{IGNORE}"));
  }
}
