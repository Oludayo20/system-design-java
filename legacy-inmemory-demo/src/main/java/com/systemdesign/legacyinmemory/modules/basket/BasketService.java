package com.systemdesign.legacyinmemory.modules.basket;

import com.systemdesign.legacyinmemory.common.AppException;
import com.systemdesign.legacyinmemory.infrastructure.InMemoryDatabase;
import com.systemdesign.legacyinmemory.modules.catalog.CatalogService;
import com.systemdesign.legacyinmemory.modules.catalog.Product;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Java port of {@code modules/basket/basket.module.js} - "Add to Cart, Remove from Cart, Cart
 * Totals, Coupons." Reads product info from {@link CatalogService} through a public method
 * call, never by reaching into catalog's table directly - that's the module boundary the
 * original's comment calls out.
 */
@Service
public class BasketService {

    private final InMemoryDatabase db;
    private final CatalogService catalog;

    public BasketService(InMemoryDatabase db, CatalogService catalog) {
        this.db = db;
        this.catalog = catalog;
    }

    public Cart addToCart(int userId, int productId, int qty) {
        Product product = catalog.listProducts().stream()
                .filter(p -> p.getId() == productId)
                .findFirst()
                .orElseThrow(() -> new AppException(404, "Product not found"));

        Cart cart = findCart(userId).orElseGet(() -> db.insert("carts", new Cart(userId, new ArrayList<>())));
        cart.getItems().add(new CartItem(productId, qty, product.getPrice(), product.getName()));
        System.out.println("[Basket] user #" + userId + " added " + qty + "x \"" + product.getName() + "\" to cart");
        return cart;
    }

    public Cart getCart(int userId) {
        return findCart(userId).orElseGet(() -> new Cart(userId, new ArrayList<>()));
    }

    public double getTotal(int userId) {
        return getCart(userId).getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQty())
                .sum();
    }

    private Optional<Cart> findCart(int userId) {
        return db.find("carts", (Cart c) -> c.getUserId() == userId);
    }
}
