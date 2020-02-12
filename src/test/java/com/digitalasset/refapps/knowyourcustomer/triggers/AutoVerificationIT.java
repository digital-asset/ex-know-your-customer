/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.triggers;

import static org.junit.Assert.*;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;
import da.refapps.knowyourcustomer.kycextension.QualityAssuranceRequest;
import da.refapps.knowyourcustomer.kycextension.ResearchResult;
import da.timeservice.timeservice.CurrentTime;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class AutoVerificationIT extends TriggerTest {
  private static final List<String> everyone =
      Arrays.asList(
          KYC_ANALYST.getValue(), KYC_REVIEWER.getValue(), KYC_QUALITYASSURANCE.getValue());
  private static final Sandbox sandbox =
      buildSandbox("Test.DA.RefApps.KnowYourCustomer.Triggers.AutoVerificationTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoVerifyTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return KYC_QUALITYASSURANCE;
  }

  @Test
  public void researchResultsAreVerifiedOnDaysDivisibleByThree() throws IOException {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    Instant dayDivisibleByThree = getInstantForDayOfTheMonth(3);
    ledger.createContract(
        OPERATOR,
        CurrentTime.TEMPLATE_ID,
        new CurrentTime(OPERATOR.getValue(), dayDivisibleByThree, everyone).toValue());

    ledger.getMatchedContract(
        KYC_QUALITYASSURANCE,
        QualityAssuranceRequest.TEMPLATE_ID,
        QualityAssuranceRequest.ContractId::new);

    ContractWithId<ResearchResult.ContractId> researchResultWithId =
        ledger.getMatchedContract(
            KYC_QUALITYASSURANCE, ResearchResult.TEMPLATE_ID, ResearchResult.ContractId::new);

    ResearchResult researchResult = ResearchResult.fromValue(researchResultWithId.record);

    assertEquals(KYC_ANALYST.getValue(), researchResult.analyst);
    assertEquals(KYC_REVIEWER.getValue(), researchResult.reviewer);
    assertEquals(KYC_QUALITYASSURANCE.getValue(), researchResult.qualityAssurance);

    assertTrue(researchResult.verifiedByQA);

    ledger.assertDidntHappen(
        KYC_QUALITYASSURANCE.getValue(),
        ContractCreated.expectContract(ResearchResult.TEMPLATE_ID, "{IGNORE}"));
  }

  @Test
  public void researchResultsAreNotVerifiedOnDaysNotDivisibleByThree() throws IOException {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();

    Instant dayNotDivisibleByThree = getInstantForDayOfTheMonth(1);
    ledger.createContract(
        OPERATOR,
        CurrentTime.TEMPLATE_ID,
        new CurrentTime(OPERATOR.getValue(), dayNotDivisibleByThree, everyone).toValue());

    ledger.getMatchedContract(
        KYC_QUALITYASSURANCE,
        QualityAssuranceRequest.TEMPLATE_ID,
        QualityAssuranceRequest.ContractId::new);

    ContractWithId<ResearchResult.ContractId> researchResultWithId =
        ledger.getMatchedContract(
            KYC_QUALITYASSURANCE, ResearchResult.TEMPLATE_ID, ResearchResult.ContractId::new);

    ResearchResult researchResult = ResearchResult.fromValue(researchResultWithId.record);

    assertEquals(KYC_ANALYST.getValue(), researchResult.analyst);
    assertEquals(KYC_REVIEWER.getValue(), researchResult.reviewer);
    assertEquals(KYC_QUALITYASSURANCE.getValue(), researchResult.qualityAssurance);

    assertFalse(researchResult.verifiedByQA);

    ledger.assertDidntHappen(
        KYC_QUALITYASSURANCE.getValue(),
        ContractCreated.expectContract(ResearchResult.TEMPLATE_ID, "{IGNORE}"));
  }

  private Instant getInstantForDayOfTheMonth(int day) {
    LocalDate localDate = LocalDate.of(2022, 1, day);
    LocalDateTime localDateTime = localDate.atStartOfDay();
    return localDateTime.toInstant(ZoneOffset.UTC);
  }
}
