package com.systemdesign.legacyinmemory.api;

import com.systemdesign.legacyinmemory.api.dto.AddToCartRequest;
import com.systemdesign.legacyinmemory.modules.basket.BasketService;
import com.systemdesign.legacyinmemory.modules.basket.Cart;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Java port of the {@code /basket/*} routes in {@code api/server.js}. */
@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addToCart(@PathVariable int userId, @RequestBody AddToCartRequest request) {
        int qty = request.qty() != null ? request.qty() : 1; // matches JS `req.body.qty ?? 1`
        Cart cart = basketService.addToCart(userId, request.productId(), qty);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @GetMapping("/{userId}")
    public Cart getCart(@PathVariable int userId) {
        return basketService.getCart(userId);
    }
}
