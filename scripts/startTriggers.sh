#!/usr/bin/env bash
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

export _JAVA_OPTIONS="-Xms8m -Xmx64m"

set -e

cleanup() {
    pids=$(jobs -p)
    echo Killing $pids
    [ -n "$pids" ] && kill $pids
}

trap "cleanup" INT QUIT TERM

if [ $# -lt 2 ]; then
    echo "${0} SANDBOX_HOST SANDBOX_PORT [DAR_FILE]"
    exit 1
fi

SANDBOX_HOST="${1}"
SANDBOX_PORT="${2}"
DAR_FILE="${3:-/home/daml/know-your-customer.dar}"

# Market setup DAML script
daml script \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --script-name DA.RefApps.KnowYourCustomer.MarketSetupScript:setupMarketForSandbox \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT}
echo "DAML script executed"

# Automatically propose license prices and automatically accept them
daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoAcceptTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party KYC_Analyst &

daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party CIP_Provider &

daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party CDD_Provider &

daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party ScreeningProvider &

# Automatically register new licenses
daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party CIP_Provider &

daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party CDD_Provider &

daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party ScreeningProvider &

# Automatically start research and register their licenses
daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoStartResearch:autoStartResearchProcessTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party KYC_Analyst &

# Automatic review and quality assurance of researchs
daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoReviewTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party KYC_Reviewer &

daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.AutoReviewAndVerification:autoVerifyTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party KYC_QA &

# Automatically merge different screenings into a research and publish the research
daml trigger \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --trigger-name DA.RefApps.KnowYourCustomer.Triggers.MergeAndPublishResearch:mergeAndPublishResearchDataTrigger \
    --ledger-host ${SANDBOX_HOST} \
    --ledger-port ${SANDBOX_PORT} \
    --ledger-party KYC_Analyst &

sleep 2
pids=$(jobs -p)
echo Waiting for $pids
wait $pids
