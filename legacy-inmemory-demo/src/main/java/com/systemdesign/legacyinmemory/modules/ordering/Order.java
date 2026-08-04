package com.systemdesign.legacyinmemory.modules.ordering;

import com.systemdesign.legacyinmemory.modules.basket.CartItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Row shape for the {@code orders} table - {@code { id, userId, items, total, status, createdAt } }. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int id;
    private int userId;
    private List<CartItem> items;
    private double total;
    private String status;
    private long createdAt;
}
