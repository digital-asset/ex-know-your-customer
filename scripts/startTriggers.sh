#!/usr/bin/env bash
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

set -e

if [ $# -lt 3 ]; then
    echo "${0} SANDBOX_HOST SANDBOX_PORT MODEL_DAR_FILE"
    exit 1
fi

SANDBOX_HOST="${1}"
SANDBOX_PORT="${2}"
MODEL_DAR_FILE="${3}"

daml script \
    --wall-clock-time \
    --dar "$MODEL_DAR_FILE" \
    --script-name DA.RefApps.KnowYourCustomer.MarketSetupScript:setupMarketForSandbox \
    --ledger-host "$SANDBOX_HOST" \
    --ledger-port "$SANDBOX_PORT"
echo "DAML script executed"

scripts/startTriggers.py "$SANDBOX_PORT"
