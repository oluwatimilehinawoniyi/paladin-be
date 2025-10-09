package com.paladin.common.enums;

import lombok.Getter;

@Getter
public enum FeatureRequestStatus {
    PENDING("Pending", "Submitted and awaiting review"),
    UNDER_REVIEW("Under Review", "Being evaluated by the team"),
    IN_PROGRESS("In Progress", "Currently being developed"),
    COMPLETED("Completed", "Feature has been implemented"),
    REJECTED("Rejected", "Will not be implemented");

    private final String displayName;
    private final String description;

    FeatureRequestStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}