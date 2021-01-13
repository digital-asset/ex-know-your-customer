/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.refapps.knowyourcustomer;

import static com.digitalasset.refapps.knowyourcustomer.utils.AppParties.ALL_PARTIES;
import static com.digitalasset.refapps.utils.EventuallyUtil.eventually;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.refapps.knowyourcustomer.utils.AppParties;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;
import com.google.protobuf.InvalidProtocolBufferException;
import da.refapps.knowyourcustomer.datalicense.LiveStreamLicense;
import da.refapps.knowyourcustomer.publication.Publication;
import da.refapps.knowyourcustomer.roles.DataLicenseProposal;
import da.refapps.knowyourcustomer.roles.DataStreamRequest;
import da.refapps.knowyourcustomer.roles.PublisherConsumerRelationship;
import da.refapps.knowyourcustomer.types.ObservationReference;
import da.refapps.knowyourcustomer.types.SubscriptionFee;
import da.refapps.knowyourcustomer.types.observationvalue.Research;
import da.refapps.knowyourcustomer.types.optionaldata.Data;
import da.refapps.knowyourcustomer.types.optionaldata.NotAvailable;
import da.timeservice.timeservice.TimeManager;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.function.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class EndToEndIT {

  private static final Path RELATIVE_DAR_PATH = Paths.get("target/know-your-customer.dar");
  private static final Party OPERATOR = new Party("Operator");
  private static final Party CIP_PROVIDER = new Party("CIP_Provider");
  private static final Party CDD_PROVIDER = new Party("CDD_Provider");
  private static final Party SCREENING_PROVIDER = new Party("ScreeningProvider");
  private static final Party BANK_1 = new Party("Bank1");
  private static final Party BANK_2 = new Party("Bank2");
  private static final Party KYC_ANALYST = new Party("KYC_Analyst");
  private static final Party KYC_REVIEWER = new Party("KYC_Reviewer");
  private static final Party KYC_QUALITY_ASSURANCE = new Party("KYC_QA");
  private static final Duration systemPeriodTime = Duration.ofSeconds(5);
  private static Sandbox sandbox =
      Sandbox.builder()
          .dar(RELATIVE_DAR_PATH)
          .parties(
              OPERATOR,
              CIP_PROVIDER,
              CDD_PROVIDER,
              SCREENING_PROVIDER,
              BANK_1,
              BANK_2,
              KYC_ANALYST,
              KYC_REVIEWER,
              KYC_QUALITY_ASSURANCE)
          .useWallclockTime()
          .timeout(Duration.ofSeconds(90))
          .setupAppCallback(Main.runBots(new AppParties(ALL_PARTIES), systemPeriodTime))
          .build();
  @ClassRule public static ExternalResource compile = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();
  private Process marketSetupAndTriggers;
  private DefaultLedgerAdapter ledger;
  private TimeManager.ContractId timeManager;

  @Before
  public void setUp() throws Throwable {
    // Valid port is assigned only after the sandbox has been started.
    // Therefore trigger has to be configured at the point where this can be guaranteed.
    File log = new File("integration-marketSetupAndTriggers.log");
    File errLog = new File("integration-marketSetupAndTriggers.err.log");
    marketSetupAndTriggers =
        new ProcessBuilder()
            .command(
                "scripts/startTriggers.sh",
                "localhost",
                Integer.toString(sandbox.getSandboxPort()),
                RELATIVE_DAR_PATH.toString())
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
            .redirectError(ProcessBuilder.Redirect.appendTo(errLog))
            .start();
    ledger = sandbox.getLedgerAdapter();
    waitForTheWholeSystemToSetup();
    timeManager = getTimeManager();
  }

  private void waitForTheWholeSystemToSetup() throws InterruptedException {
    eventually(
        () ->
            ledger.getCreatedContractId(
                KYC_ANALYST, LiveStreamLicense.TEMPLATE_ID, LiveStreamLicense.ContractId::new));
  }

  private TimeManager.ContractId getTimeManager() {
    return ledger.getCreatedContractId(
        OPERATOR, TimeManager.TEMPLATE_ID, TimeManager.ContractId::new);
  }

  @After
  public void tearDown() {
    marketSetupAndTriggers.destroyForcibly();
  }

  @Test
  public void endToEndIT() throws InvalidProtocolBufferException, InterruptedException {
    consumeInitialContracts();

    continueTime();

    Research research = getResearchFor(BANK_1);
    assertTrue(research.researchData.researchCip instanceof Data);
    assertTrue(research.researchData.researchCdd instanceof NotAvailable);
    assertTrue(research.researchData.researchScreening instanceof Data);

    PublisherConsumerRelationship.ContractId analystWithBank2 =
        ledger.getCreatedContractId(
            BANK_2,
            PublisherConsumerRelationship.TEMPLATE_ID,
            PublisherConsumerRelationship.ContractId::new);
    ledger.exerciseChoice(
        BANK_2,
        analystWithBank2.exerciseRequestStandardAnnualStream(
            new ObservationReference("ACME", true, true, true)));

    DataStreamRequest.ContractId streamRequest =
        ledger.getCreatedContractId(
            KYC_ANALYST, DataStreamRequest.TEMPLATE_ID, DataStreamRequest.ContractId::new);
    ledger.exerciseChoice(
        KYC_ANALYST,
        streamRequest.exerciseDataStreamRequest_Propose(new SubscriptionFee(BigDecimal.TEN)));

    DataLicenseProposal.ContractId licenseProposal =
        ledger.getCreatedContractId(
            BANK_2, DataLicenseProposal.TEMPLATE_ID, DataLicenseProposal.ContractId::new);
    ledger.exerciseChoice(BANK_2, licenseProposal.exerciseDataLicenseProposal_Accept());

    research = eventually(() -> getResearchFor(BANK_2));
    assertTrue(research.researchData.researchCip instanceof Data);
    assertTrue(research.researchData.researchCdd instanceof Data);
    assertTrue(research.researchData.researchScreening instanceof Data);
  }

  private void consumeInitialContracts() {
    ledger.observeMatchingContracts(
        KYC_ANALYST,
        DataStreamRequest.TEMPLATE_ID,
        DataStreamRequest::fromValue,
        true,
        isStreamRequestBetween(KYC_ANALYST, CIP_PROVIDER),
        isStreamRequestBetween(KYC_ANALYST, CDD_PROVIDER),
        isStreamRequestBetween(KYC_ANALYST, SCREENING_PROVIDER),
        isStreamRequestBetween(BANK_1, KYC_ANALYST));
    ledger.observeMatchingContracts(
        KYC_ANALYST,
        Publication.TEMPLATE_ID,
        Publication::fromValue,
        true,
        isPublishedBy(CIP_PROVIDER),
        isPublishedBy(CDD_PROVIDER),
        isPublishedBy(SCREENING_PROVIDER));
  }

  private Predicate<Publication> isPublishedBy(Party party) {
    return x -> party.getValue().equals(x.publisher.party);
  }

  private Predicate<DataStreamRequest> isStreamRequestBetween(Party consumer, Party publisher) {
    return x ->
        consumer.getValue().equals(x.consumer.party)
            && publisher.getValue().equals(x.publisher.party);
  }

  private void continueTime() {
    try {
      ledger.exerciseChoice(OPERATOR, timeManager.exerciseContinue());
    } catch (InvalidProtocolBufferException e) {
      fail("Could not continue time. Reason: " + e.getMessage());
    }
  }

  private Research getResearchFor(Party party) {
    Publication publication = getPublicationFor(party);
    return Research.fromValue(publication.observation.value.toValue());
  }

  private Publication getPublicationFor(Party party) {
    ContractWithId<Publication.ContractId> contractWithId =
        ledger.getMatchedContract(party, Publication.TEMPLATE_ID, Publication.ContractId::new);
    return Publication.fromValue(contractWithId.record);
  }
}
