package com.example.cart.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A shopping cart.
 */
@Value
@JsonDeserialize
public final class CartView {
    /**
     * The ID of the shopping cart.
     */
    public final String id;

    /**
     * The list of items in the cart.
     */
    public final List<CartItem> items;

    /**
     * Whether this cart has been checked out.
     */
    public final boolean checkedOut;

    /**
     * When this cart was checked out.
     */
    public final Optional<Instant> checkoutDate;

    @JsonCreator
    public CartView(String id, List<CartItem> items, Optional<Instant> checkoutDate) {
        this.id = Preconditions.checkNotNull(id, "id");
        this.items = Preconditions.checkNotNull(items, "items");
        this.checkoutDate = checkoutDate;
        this.checkedOut = checkoutDate.isPresent();
    }

    public boolean hasItem(String itemId) {
        return items.stream().anyMatch(item -> item.getItemId().equals(itemId));
    }

    public Optional<CartItem> get(String itemId) {
        return items.stream().filter(item -> item.getItemId().equals(itemId)).findFirst();
    }
}
