/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.daml.product.refapps.knowyourcustomer.triggers;

import static com.daml.product.refapps.knowyourcustomer.assertions.Assert.assertContractsCreatedExactlyNTimes;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.ledger.DefaultLedgerAdapter;
import com.daml.ledger.javaapi.data.Party;
import da.refapps.knowyourcustomer.publication.Publication;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class MergeAndSendToReviewIT extends TriggerTest {

  private static final Sandbox sandbox =
      buildSandbox("DA.RefApps.KnowYourCustomer.Triggers.Tests.MergeAndSendToReviewTestSetup");

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
  public void testPublicationsAreMergedIntoAResearchAndSentForReview() {
    DefaultLedgerAdapter ledger = sandbox.getLedgerAdapter();
    assertContractsCreatedExactlyNTimes(KYC_ANALYST, ledger, 2, Publication.TEMPLATE_ID);
  }
}
