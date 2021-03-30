/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer.triggers;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.testing.junit4.Sandbox;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class RepublishResearchOnStartedDataStreamIT extends TriggerTest {

  private static final Sandbox sandbox =
      buildSandbox(
          "DA.RefApps.KnowYourCustomer.Triggers.Tests.RepublishResearchOnStartedDataStreamTestSetup");

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

  @Ignore
  @Test
  public void testReviewedResearchIsRepublishedOnDataStream() {
    PublishResearchOnEmptyDataStreamIT.doTestReviewedResearchIsPublished(sandbox, 2);
  }
}
