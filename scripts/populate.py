#!/usr/bin/env python3
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

import argparse
import logging

from damlassistant import run_script, wait_for_port


dar = 'target/know-your-customer.dar'
script_name = 'DA.RefApps.KnowYourCustomer.MarketSetupScript:setupMarketForSandbox'

parser = argparse.ArgumentParser()
parser.add_argument('ledger_port')
args = parser.parse_args()

logging.basicConfig(level=logging.DEBUG)

wait_for_port(port=args.ledger_port, timeout=30)
run_script(dar, script_name, args.ledger_port)
