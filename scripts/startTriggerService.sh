#!/usr/bin/env bash
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

set -e

TRIGGER_SERVICE_HOST="127.0.0.1"
TRIGGER_SERVICE_PORT=8088

cleanup() {
    pids=$(jobs -p)
    echo Killing $pids
    [ -n "$pids" ] && kill $pids
}

waitForTriggerService() {
    until curl ${TRIGGER_SERVICE_HOST}:${TRIGGER_SERVICE_PORT}/livez
    do
        sleep 0.1
    done
}

startTrigger() {
    curl -X POST -H "Content-Type: application/json" \
        -d "{ \"triggerName\": \"${1}\", \"party\": \"${2}\", \"applicationId\": \"my-app-id\" }" \
        ${TRIGGER_SERVICE_HOST}:${TRIGGER_SERVICE_PORT}/v1/triggers
}

trap "cleanup" INT QUIT TERM

if [ $# -lt 2 ]; then
    echo "${0} SANDBOX_HOST SANDBOX_PORT [DAR_FILE]"
    exit 1
fi

SANDBOX_HOST="${1}"
SANDBOX_PORT="${2}"
DAR_FILE="${3}"

PACKAGE_ID=$(daml damlc inspect ${3} | grep package | cut -d" " -f2)

# Market setup DAML script
daml script \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --script-name DA.RefApps.KnowYourCustomer.MarketSetupScript:setupMarketForSandbox \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT}
echo "DAML script executed"

daml trigger-service \
    --dar target/know-your-customer.dar \
    --ledger-host localhost \
    --ledger-port 6865 &

waitForTriggerService
echo "Trigger service running."

# Automatically propose license prices and automatically accept them
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoAcceptTrigger \
    KYC_Analyst

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger \
    CIP_Provider

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger \
    CDD_Provider

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger \
    ScreeningProvider

# Automatically register new licenses
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger \
    CIP_Provider

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger \
    CDD_Provider

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger \
    ScreeningProvider

# Automatically start research and register their licenses
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoStartResearch:autoStartResearchProcessTrigger \
    KYC_Analyst

# Automatic review and quality assurance of researchs
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoReviewTrigger \
    KYC_Reviewer

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoVerifyTrigger \
    KYC_QA

# Automatically merge different screenings into a research and publish the research
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.MergeAndPublishResearch:mergeAndPublishResearchDataTrigger \
    KYC_Analyst

# Time management
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.TimeUpdater:timeUpdaterTrigger \
    Operator

# Publishing
startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.Publisher:cipTrigger \
    CIP_Provider

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.Publisher:cddTrigger \
    CDD_Provider

startTrigger \
    ${PACKAGE_ID}:DA.RefApps.KnowYourCustomer.Triggers.Publisher:screeningTrigger \
    ScreeningProvider

sleep 2
pids=$(jobs -p)
echo Waiting for $pids
wait $pids
