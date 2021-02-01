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
    until curl "$TRIGGER_SERVICE_HOST:$TRIGGER_SERVICE_PORT/livez"
    do
        sleep 1
    done
}

startTriggerFromThisPackage() {
    PAYLOAD=$(printf '{ "triggerName": "%s", "party": "%s", "applicationId": "my-app-id" }' "${1}" "${2}")
    curl -X POST -H "Content-Type: application/json" \
        -d "$PAYLOAD" \
        "$TRIGGER_SERVICE_HOST:$TRIGGER_SERVICE_PORT/v1/triggers"
}

getPackageId() {
    daml damlc inspect-dar --json "$DAR_FILE" | jq -j ".main_package_id"
}

trap "cleanup" INT QUIT TERM

if [ $# -lt 3 ]; then
    echo "${0} SANDBOX_HOST SANDBOX_PORT DAR_FILE"
    exit 1
fi

SANDBOX_HOST="${1}"
SANDBOX_PORT="${2}"
DAR_FILE="${3}"

PACKAGE_ID=$(getPackageId)

daml script \
    --wall-clock-time \
    --dar "$DAR_FILE" \
    --script-name DA.RefApps.KnowYourCustomer.MarketSetupScript:setupMarketForSandbox \
    --ledger-host "$SANDBOX_HOST" \
    --ledger-port "$SANDBOX_PORT"
echo "DAML script executed"

daml trigger-service \
    --dar "$DAR_FILE" \
    --ledger-host localhost \
    --ledger-port 6865 &

waitForTriggerService
echo "Trigger service running."

# Automatically propose license prices and automatically accept them
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoAcceptTrigger" \
    KYC_Analyst

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger" \
    CIP_Provider

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger" \
    CDD_Provider

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger" \
    ScreeningProvider

# Automatically register new licenses
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger" \
    CIP_Provider

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger" \
    CDD_Provider

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger" \
    ScreeningProvider

# Automatically start research and register their licenses
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoStartResearch:autoStartResearchProcessTrigger" \
    KYC_Analyst

# Automatic review and quality assurance of researchs
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoReviewTrigger" \
    KYC_Reviewer

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoVerifyTrigger" \
    KYC_QA

# Automatically merge different screenings into a research and publish the research
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.MergeAndPublishResearch:mergeAndPublishResearchDataTrigger" \
    KYC_Analyst

# Time management
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.TimeUpdater:timeUpdaterTrigger" \
    Operator

# Publishing
startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.Publisher:cipTrigger" \
    CIP_Provider

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.Publisher:cddTrigger" \
    CDD_Provider

startTriggerFromThisPackage \
    "$PACKAGE_ID:DA.RefApps.KnowYourCustomer.Triggers.Publisher:screeningTrigger" \
    ScreeningProvider

sleep 2
pids=$(jobs -p)
echo Waiting for $pids
wait $pids
