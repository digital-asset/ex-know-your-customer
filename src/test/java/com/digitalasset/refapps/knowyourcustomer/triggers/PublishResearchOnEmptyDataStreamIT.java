/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.triggers;

import static com.digitalasset.refapps.knowyourcustomer.assertions.Assert.assertContractsCreatedExactlyNTimes;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;
import da.refapps.knowyourcustomer.datastream.DataStream;
import da.refapps.knowyourcustomer.kycextension.ResearchProcess;
import da.refapps.knowyourcustomer.publication.Publication;
import da.refapps.knowyourcustomer.types.CipData;
import da.refapps.knowyourcustomer.types.ScreeningData;
import da.refapps.knowyourcustomer.types.observationvalue.Research;
import da.refapps.knowyourcustomer.types.optionaldata.Data;
import da.refapps.knowyourcustomer.types.optionaldata.NotAvailable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class PublishResearchOnEmptyDataStreamIT extends TriggerTest {

  private static final Sandbox sandbox =
      buildSandbox(
          "DA.RefApps.KnowYourCustomer.Triggers.Tests.PublishResearchOnEmptyDataStreamTestSetup");

  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Override
  protected int getSandboxPort() {
    return sandbox.getSandboxPort();
  }

  @Override
  protected String getTriggerName() {
    return "DA.RefApps.KnowYourCustomer.Triggers.MergeAndPublishResearch:mergeAndPublishResearchDataTrigger";
  }

  @Override
  protected Party getTriggerParty() {
    return KYC_ANALYST;
  }

  @Test
  public void testReviewedResearchIsPublishedOnEmptyDataStream() {
    doTestReviewedResearchIsPublished(sandbox, 1);
  }

  static void doTestReviewedResearchIsPublished(Sandbox sandbox, int numberOfDataStreams) {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();
    assertContractsCreatedExactlyNTimes(
        KYC_ANALYST, ledger, numberOfDataStreams, DataStream.TEMPLATE_ID);
    assertContractsCreatedExactlyNTimes(KYC_ANALYST, ledger, 2, ResearchProcess.TEMPLATE_ID);
    ledger.getMatchedContract(KYC_ANALYST, Publication.TEMPLATE_ID, cid -> null);
    ledger.getMatchedContract(KYC_ANALYST, Publication.TEMPLATE_ID, cid -> null);
    Research research = getResearch(ledger);

    assertThat(research.researchDataValue.researchCip, instanceOf(Data.class));
    Data<CipData> cip = (Data<CipData>) research.researchDataValue.researchCip;
    assertEquals(cip.value.tin, "TIN1");

    assertThat(research.researchDataValue.researchScreening, instanceOf(Data.class));
    Data<ScreeningData> screening =
        (Data<ScreeningData>) research.researchDataValue.researchScreening;
    assertEquals(screening.value.ofac, "Not listed1");

    assertThat(research.researchDataValue.researchCdd, instanceOf(NotAvailable.class));
  }

  private static Research getResearch(DefaultLedgerAdapter ledger) {
    ContractWithId<Publication> mergedPublicationMatched =
        ledger.getMatchedContract(KYC_ANALYST, Publication.TEMPLATE_ID, cid -> null);
    Publication mergedPublication = Publication.fromValue(mergedPublicationMatched.record);
    return Research.fromValue(mergedPublication.observation.value.toValue());
  }
}
