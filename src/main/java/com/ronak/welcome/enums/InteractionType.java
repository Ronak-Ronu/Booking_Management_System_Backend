package com.ronak.welcome.enums;


import lombok.Getter;
import lombok.Setter;

@Getter
public enum InteractionType {
    VIEW(0.1, "User viewed item"),
    SEARCH(0.2, "User searched for similar items"),
    CLICK(0.3, "User clicked on recommendation"),
    FAVORITE(0.5, "User added to favorites"),
    SHARE(0.6, "User shared the item"),
    REVIEW(0.7, "User left a review"),
    BOOK(1.0, "User made a booking"),
    CANCEL(-0.3, "User cancelled a booking");

    private final double defaultWeight;
    private final String description;

    InteractionType(double defaultWeight, String description) {
        this.defaultWeight = defaultWeight;
        this.description = description;
    }

    /**
     * Returns whether this interaction type is positive or negative
     */
    public boolean isPositive() {
        return defaultWeight > 0;
    }

    /**
     * Returns interaction types sorted by weight (strongest first)
     */
    public static InteractionType[] getByStrength() {
        return new InteractionType[]{BOOK, REVIEW, SHARE, FAVORITE, CLICK, SEARCH, VIEW, CANCEL};
    }
}
