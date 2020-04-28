#!/usr/bin/env bash
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

cleanup() {
    pids=$(jobs -p)
    echo Killing $pids
    [ -n "$pids" ] && kill $pids
}

trap "cleanup" INT QUIT TERM

DAR_FILE="$1"
SCRIPT_NAME="$2"

daml sandbox \
    --port 6865 \
    "${DAR_FILE}" &

CURRENT_DIR=$(dirname "$0")
"$CURRENT_DIR/waitForSandbox.sh" localhost 6865

echo "Starting script: ${SCRIPT_NAME}"
daml script \
    --wall-clock-time \
    --dar "${DAR_FILE}" \
    --script-name "${SCRIPT_NAME}" \
    --ledger-host localhost \
    --ledger-port 6865