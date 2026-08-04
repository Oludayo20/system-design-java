package com.systemdesign.legacyinmemory.modules.basket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single line item in a {@link Cart} - {@code { productId, qty, price, name } }. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private int productId;
    private int qty;
    private double price;
    private String name;
}
