#!/usr/bin/env python3
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

import argparse
import logging
import time

from damlassistant import get_package_id, start_trigger_service_in_background, kill_process, \
    add_trigger_to_service, wait_for_port, catch_signals, DEFAULT_TRIGGER_SERVICE_PORT

dar = 'target/know-your-customer-triggers.dar'

triggers_with_parties = [
    ("KYC_Analyst", "DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoAcceptTrigger"),
    ("CIP_Provider", "DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger"),
    ("CDD_Provider", "DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger"),
    ("ScreeningProvider", "DA.RefApps.KnowYourCustomer.Triggers.AutoProposeAndAccept:autoProposeTrigger"),
    ("CIP_Provider", "DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger"),
    ("CDD_Provider", "DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger"),
    ("ScreeningProvider", "DA.RefApps.KnowYourCustomer.Triggers.AutoRegisterLicense:automaticLicenseRegistrarTrigger"),
    ("KYC_Analyst", "DA.RefApps.KnowYourCustomer.Triggers.AutoStartResearch:autoStartResearchProcessTrigger"),
    ("KYC_Analyst", "DA.RefApps.KnowYourCustomer.Triggers.MergeAndPublishResearch:mergeAndPublishResearchDataTrigger"),
    ("Operator", "DA.RefApps.KnowYourCustomer.Triggers.TimeUpdater:timeUpdaterTrigger"),
    ("CIP_Provider", "DA.RefApps.KnowYourCustomer.Triggers.Publisher:cipTrigger"),
    ("CDD_Provider", "DA.RefApps.KnowYourCustomer.Triggers.Publisher:cddTrigger"),
    ("ScreeningProvider", "DA.RefApps.KnowYourCustomer.Triggers.Publisher:screeningTrigger"),
]

parser = argparse.ArgumentParser()
parser.add_argument('ledger_port')
args = parser.parse_args()

logging.basicConfig(level=logging.DEBUG)

wait_for_port(port=args.ledger_port, timeout=30)

service = start_trigger_service_in_background(dar=dar, ledger_port=args.ledger_port)
try:
    catch_signals()
    package_id = get_package_id(dar)
    wait_for_port(port=DEFAULT_TRIGGER_SERVICE_PORT, timeout=30)
    for party, triggerName in triggers_with_parties:
        add_trigger_to_service(party=party, package_id=package_id, trigger=triggerName)

    def print_message_after_triggers_started(m: str):
        time.sleep(3)
        print(m)

    print_message_after_triggers_started('\nPress Ctrl+C to stop...')
    service.wait()
    logging.error(f"Trigger service died unexpectedly:\n{service.stderr}")
finally:
    kill_process(service)
