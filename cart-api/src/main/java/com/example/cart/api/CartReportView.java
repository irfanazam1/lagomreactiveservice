package com.example.cart.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Value;

import java.time.Instant;

/**
 * A shopping cart report.
 */
@Value
@JsonDeserialize
public class CartReportView {
    /**
     * The ID of the shopping cart.
     */
    public final String id;

    /**
     * The shopping cart creation date
     */
    public final Instant creationDate;

    public final Instant checkoutDate;

    @JsonCreator
    public CartReportView(String id, Instant creationDate, Instant checkoutDate) {
        this.id = id;
        this.creationDate = creationDate;
        this.checkoutDate = checkoutDate;
    }
}
