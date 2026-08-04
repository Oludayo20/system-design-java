package com.systemdesign.legacyinmemory.modules.basket;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Row shape for the {@code carts} table - {@code { userId, items[] } }. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private int userId;
    private List<CartItem> items = new ArrayList<>();
}
