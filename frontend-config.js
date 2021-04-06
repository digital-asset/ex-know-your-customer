/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import { DamlLfValue } from '@da/ui-core';

export const version = {
    schema: 'navigator-config',
    major: 2,
    minor: 0
};

// --- Creating views --------------------------------------------------------------------

function publisherProvidedView(publisher) {
    return createTab("Provided KYC streams", "DataStream@",
        [
            createCol("reference", "Customer name", 140, r => showKycReference(r.reference || r.observation.label)),
            createCol("consumers", "Consumers", 80, r => showConsumers(r.consumers)),
            createCol("started", "Started", 80, r => (r.observation === undefined) ? "No" : "Yes"),
            createCol("observation", "Data", 160, r => r.observation ? showKycObservationValue(r.observation) : "-") // can be empty
        ],
        {
            field: "argument.publisher.party",
            value: publisher
        }
    );
}

function publisherDataView(publisher, withResearchFlags) {
    var cols = [
        createIdCol(),
        createCol("published", "Published", 80, r => r.published),
        createCol("consumer", "Consumer", 80),
        createCol("reference", "Customer name", 140, r => showKycReference(r.observation.label)),
        createCol("time", "Time", 80, r => r.observation.time),
        createCol("observation", "Data", 160, r => showKycObservationValue(r.observation))
    ];
    return createTab("Sent KYC data", ":Publication@",
        cols,
        {
            field: "argument.publisher.party",
            value: publisher
        }
    );
}

function consumerView(consumer, withResearchFlags) {
    var cols = [
        createIdCol(),
        createCol("published", "Published", 80, r => r.published),
        createCol("publisher"),
        createCol("reference", "Customer name", 140, r => showKycReference(r.observation.label)),
        createCol("observation", "Data", 160, r => showKycObservationValue(r.observation))
    ];
    return createTab("Received KYC data", ":Publication@",
        cols,
        {
            field: "argument.consumer.party",
            value: consumer
        }
    );
}

function complaintsNonPerformanceView(consumer) {
    return createTab("Complaints: NonPerformance", "NonPerformance",
        [
            createCol("claimed", "Claimed", 80, r => r.claimed),
            createCol("starting", "Starting", 80, r => r.licenseData.starting),
        ],
        {
            field: "argument.licenseData.consumer.party",
            value: consumer
        });
}

function complaintsStaleDataView(consumer) {
    return createTab("Complaints: StaleData", "StalePublication",
        [
            createCol("claimed", "Claimed", 80, r => r.claimed),
            createCol("published", "Published", 80, r => r.publication.published),
            createCol("stale", "Stale allowed", 80, r => prettyMs(r.licenseData.stale.microseconds)),
        ],
        {
            field: "argument.licenseData.consumer.party",
            value: consumer
        });
}

function complaintsDisputedStreamView(consumer) {
    return createTab("Complaints: data quality", "DisputedStreamQuality",
        [
            createCol("claimed", "Claimed", 80, r => r.claimed),
            createCol("publisher", "KYC provider", 80, r => r.publisher.party),
            createCol("reference", "Customer name", 140, r => showKycReference(r.publication.observation.label)),
            createCol("published", "Published", 80, r => r.publication.published),
            createCol("description", "Description", 80, r => r.description),
        ]);
}

const timeManagerView = createTab("Time Management", "TimeManager",
    [
        createCol("operator", "Operator", 80, r => r.operator)
    ]
);

const timeConfigurationView = createTab("Time Configuration", "TimeConfiguration",
    [
        createCol("operator", "Operator", 80, r => r.operator),
        createCol("modelPeriodTime", "ModelPeriodTime", 80, r => prettyMs(r.modelPeriodTime.microseconds)),
        createCol("isRunning", "Running", 80, r => {
            return r.isRunning ? "Running" : "Stopped";
        })
    ]
);

const currentTimeView = createTab("Current Time", "CurrentTime",
    [
        createCol("time", "Time", 80, r => r.currentTime)
    ]
);

const dataSourceView = createTab("Data Sources", "DataSource",
    [
        createCol("owner", "Owner", 80, r => r.owner),
        createCol("operator", "Operator", 80, r => r.operator),
        createCol("reference", "Customer name", 140, r => showKycReference(r.reference || r.observation.label)),
    ]
);

const dataProviderView = createTab("Data Provider Role", "DataProviderRole",
    [
        createCol("operator", "Operator", 80, r => r.operator),
        createCol("dataProvider", "Data Provider", 80, r => r.dataProvider)
    ]
);

function relationships(party) {
    return createTab("Relationships", "PublisherConsumerRelationship",
        [
            createCol("myrole", "My Role", 20, r => {
                if (r.publisher.party === party) {
                    return "KYC data publisher";
                } else if (r.consumer.party === party) {
                    return "KYC data consumer";
                } else {
                    return "Unknown role";
                }
            }),
            createCol("counterparty", "Counterparty", 80, r => {
                if (r.publisher.party === party) {
                    return `${r.consumer.party}`;
                } else if (r.consumer.party === party) {
                    return `${r.publisher.party}`;
                } else {
                    return "Unknown counterparty";
                }
            }),
            createCol("licenses", "Licenses", 90, r => {
                if (r.publisher.party === party) {
                    return "-";
                } else if (r.consumer.party === party) {
                    return "Click to request KYC data or to renew license...";
                } else {
                    return "-";
                }
            }),
        ]
    );
}

function streamRequestsView(party) {
    return createTab("KYC Requests", "DataStreamRequest",
        [
            createCol("type", "In/Out", 80, r => {
                if (r.publisher.party === party) {
                    return "Received";
                } else if (r.consumer.party === party) {
                    return "Sent";
                } else {
                    return "-";
                }
            }),
            createCol("counterparty", "Counterparty", 100, r => {
                if (r.publisher.party === party) {
                    return r.consumer.party;
                } else if (r.consumer.party === party) {
                    return r.publisher.party;
                } else {
                    return "Unknown counterparty";
                }
            }),
            createCol("reference", "Customer name", 140, r => showKycReference(r.reference || r.observation.label)),
        ]
    );
}

function licenseProposalView(party) {
    return createTab("KYC Price Proposals", "DataLicenseProposal",
        [
            createCol("type", "In/Out", 80, r => {
                if (r.publisher.party === party) {
                    return "Sent";
                } else if (r.consumer.party === party) {
                    return "Received";
                } else {
                    return "-";
                }
            }),
            createCol("counterparty", "Counterparty", 100, r => {
                if (r.publisher.party === party) {
                    return r.consumer.party;
                } else if (r.consumer.party === party) {
                    return r.publisher.party;
                } else {
                    return "Unknown counterparty";
                }
            }),
            createCol("reference", "Customer name", 140, r => showKycReference(r.reference || r.observation.label)),
            createCol("price", "Price", 80, r => r.price),
        ]
    );
}

function licenseView(party) {
    return createTab("KYC Licenses", "License@",
        [
            createCol("type", "Description", 200, r => {
                const publisherParty = r.licenseData.publisher.party;
                const consumerParty = r.licenseData.consumer.party
                if (publisherParty === party) {
                    return "Approved for consumer '" + consumerParty + "'";
                } else if (consumerParty === party) {
                    return "Received from KYC data publisher '" + publisherParty + "'";
                } else {
                    return "-";
                }
            }),
            createCol("reference", "Customer name", 140, r => showKycReference(r.licenseData.reference)),
            createCol("price", "Price", 80, r => r.licenseData.price),
            createCol("live", "Status", 80, r => {
                if (r.began != null) {
                    return "Live"
                } else {
                    return "-";
                }
            })
        ]
    );
}

// --- Assigning vievs to parties --------------------------------------------------------------------

export const customViews = (userId, party, role) => {
    function partyIs(partyName) {
        return party === partyName || userId === partyName;
    }
    if (partyIs('Operator')) {
        return {
            timeManagerView,
            timeConfigurationView,
            currentTimeView
        };
    }
    const reviewsViewInstance = reviewsView()
    const qualityAssuranceInstance = qualityAssuranceView()

    const publisherDataViewInstance = publisherDataView(party, false);
    const publisherProvidedViewInstance = publisherProvidedView(party);
    const relationshipsViewInstance = relationships(party);
    const streamRequestsViewInstance = streamRequestsView(party);
    const licenseProposalViewInstance = licenseProposalView(party);
    const licenseViewInstance = licenseView(party);
    const complaintsDisputedStreamInstance = complaintsDisputedStreamView(party);
    if (partyIs('CIP_Provider')) {
        return {
            relationshipsViewInstance,
            dataProviderView,
            publisherProvidedViewInstance,
            publisherDataViewInstance,
            streamRequestsViewInstance,
            licenseProposalViewInstance,
            licenseViewInstance,
            complaintsDisputedStreamInstance,
            dataSourceView,
            currentTimeView
        };
    }

    if (partyIs('CDD_Provider')) {
        return {
            relationshipsViewInstance,
            dataProviderView,
            publisherProvidedViewInstance,
            publisherDataViewInstance,
            streamRequestsViewInstance,
            licenseProposalViewInstance,
            licenseViewInstance,
            complaintsDisputedStreamInstance,
            dataSourceView,
            currentTimeView
        };
    }

    if (partyIs('ScreeningProvider')) {
        return {
            relationshipsViewInstance,
            dataProviderView,
            publisherProvidedViewInstance,
            publisherDataViewInstance,
            streamRequestsViewInstance,
            licenseProposalViewInstance,
            licenseViewInstance,
            complaintsDisputedStreamInstance,
            dataSourceView,
            currentTimeView
        };
    }
    const publisherDataViewWithFlagsInstance = publisherDataView(party, true);
    const consumerViewInstance = consumerView(party, false);
    const consumerViewWithFlagsInstance = consumerView(party, true);
    if (partyIs('KYC_Analyst')) {
        return {
            relationshipsViewInstance,
            consumerViewInstance,
            publisherProvidedViewInstance,
            publisherDataViewWithFlagsInstance,
            streamRequestsViewInstance,
            licenseProposalViewInstance,
            licenseViewInstance,
            reviewsViewInstance,
            qualityAssuranceInstance,
            complaintsDisputedStreamInstance,
            currentTimeView
        };
    }

    if (partyIs('Bank1') || partyIs('Bank2')) {
        return {
            relationshipsViewInstance,
            consumerViewWithFlagsInstance,
            streamRequestsViewInstance,
            licenseProposalViewInstance,
            licenseViewInstance,
            complaintsDisputedStreamInstance,
            currentTimeView
        };
    } else {
        return {
        };
    }
};

// --- Helpers --------------------------------------------------------------------

/**
 title, width and proj are optional

 if proj is null and key is "id" then it will default to the contract id
 if proj is null and key is not "id" then it will default to stringified single or array value of rowData.key
*/
function createCol(key, title = toTitle(key), width = 80, proj) {
    return {
        key: key,
        title: title,
        createCell: ({ rowData }) => {
            return {
                type: "text",
                value: valueFunction(rowData, key, proj)
            }
        },
        sortable: true,
        width: width,
        weight: 0,
        alignment: "left"
    };
}

function createIdCol() {
    return createCol("id", "Contract ID", 60);
}

function createTab(name, templateId, columns, additionalFilter) {
    let filter;
    if (additionalFilter == null) {
        filter =
            [
                {
                    field: "template.id",
                    value: templateId
                }
            ]
    } else {
        filter =
            [
                {
                    field: "template.id",
                    value: templateId
                },
                additionalFilter
            ]
    }
    return {
        type: "table-view",
        title: name,
        source: {
            type: "contracts",
            filter: filter,
            search: "",
            sort: [
                {
                    field: "id",
                    direction: "ASCENDING"
                }
            ]
        },
        columns: columns
    };
}

/**
 * A convenient function that unpacks a wrapped value. If the value is not
 * wrapped this is a no-op.
 * @example
 * unpack({unpack: 12.34}); // returns 12.34
 * unpack(12.34); // returns 12.34
 */
function unpack(value) {
    return (value && value.unpack)
        ? value.unpack
        : value;
}

/**
 * Will return 0 for empty string or strings with whitespace. This is due
 * to the documented behaviour of Number.
 */
function formatIfNum(val) {
    const n = Number(val);
    if (Number.isNaN(n)) return val;
    else return n.toLocaleString();
}

function valueFunction(rowData, key, proj) {
    return (
        proj == null
            ?
            (
                Array.isArray(DamlLfValue.toJSON(rowData.argument)[key])
                    ?
                    DamlLfValue.toJSON(rowData.argument)[key].join(", ")
                    :
                    (
                        key === "id"
                            ?
                            rowData.id
                            :
                            (
                                'party' in DamlLfValue.toJSON(rowData.argument)[key]
                                    ?
                                    DamlLfValue.toJSON(rowData.argument)[key].party
                                    :
                                    formatIfNum(DamlLfValue.toJSON(rowData.argument)[key])
                            )
                    )
            )
            :
            formatIfNum(unpack(proj(DamlLfValue.toJSON(rowData.argument)))));
}

// inserts spaces into the usually camel-case key
// e.g. "assetISINCode" -> "Asset ISIN Code"
function toTitle(key) {
    const spaced = key.replace(/([^A-Z])([A-Z])/g, '$1 $2').replace(/([A-Z])([A-Z][^A-Z])/g, '$1 $2');
    return spaced[0].toUpperCase() + spaced.substr(1)
}

function showConsumers(xs) {
    return xs.map(showParty).join(", ");
}

function showParty(member) {
    return member.party;
}

function showKycObservationValue(observation) {
    if (observation.value.CIP) {
        return `${observation.value.CIP.tin}`;
    } else if (observation.value.CDD) {
        return `${observation.value.CDD.revenue}`;
    } else if (observation.value.Screening) {
        return `${observation.value.Screening.ofac}`;
    } else if (observation.value.Research) {
        const cip = observation.value.Research.researchData.researchCip.hasOwnProperty("NotAvailable") ? "N/A" : observation.value.Research.researchData.researchCip.Data.value.tin;
        const cdd = observation.value.Research.researchData.researchCdd.hasOwnProperty("NotAvailable") ? "N/A" : observation.value.Research.researchData.researchCdd.Data.value.revenue;
        const screening = observation.value.Research.researchData.researchScreening.hasOwnProperty("NotAvailable") ? "N/A" : observation.value.Research.researchData.researchScreening.Data.value.ofac;
        return `${cip}, ${cdd}, ${screening}`;
    } else {
        return "N/A";
    }
}

// could use pretty-ms or humanize-duration js package
function prettyMs(microSeconds) {
    const seconds = Math.floor(microSeconds / 1000000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    return hours + " hours " + (minutes % 60) + " minutes";
}

function showKycReference(reference) {
    const cip = reference.includeCIP ? "CIP" : ""; // TODO remove ternary ops (flooding console with bools)
    const cdd = reference.includeCDD ? "CDD" : "";
    const screening = reference.includeScreening ? "Screening" : "";
    const checks = [cip, cdd, screening].filter(el => el != "");
    return `${reference.customerName} (${checks.join(", ")})`
}
