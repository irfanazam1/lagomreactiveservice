package com.example.cart.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

/**
 * An item in a shopping cart.
 */
@Value
@JsonDeserialize
public final class CartItem {
    /**
     * The ID of the product.
     */
    public final String itemId;
    /**
     * The quantity of this product in the cart.
     */
    public final int quantity;

    @JsonCreator
    public CartItem(String itemId, int quantity) {
        this.itemId = Preconditions.checkNotNull(itemId, "productId");
        this.quantity = quantity;
    }
}
