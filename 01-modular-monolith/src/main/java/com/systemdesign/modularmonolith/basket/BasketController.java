package com.systemdesign.modularmonolith.basket;

import com.systemdesign.modularmonolith.basket.dto.AddItemRequest;
import com.systemdesign.modularmonolith.basket.dto.BasketView;
import com.systemdesign.modularmonolith.identity.AuthenticatedUser;
import com.systemdesign.modularmonolith.shared.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Mirrors {@code src/modules/basket/basket.controller.ts}. Requires a valid bearer token for
 * every route (see {@code identity.security.SecurityConfig}) -- the equivalent of
 * {@code @UseGuards(JwtAuthGuard)} on the whole controller.
 */
@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    @GetMapping
    public BasketView getBasket(@CurrentUser AuthenticatedUser user) {
        return basketService.getBasket(user.userId());
    }

    @PostMapping("/items")
    public BasketView addItem(@CurrentUser AuthenticatedUser user, @Valid @RequestBody AddItemRequest dto) {
        return basketService.addItem(user.userId(), dto);
    }

    @DeleteMapping("/items/{productId}")
    public BasketView removeItem(@CurrentUser AuthenticatedUser user, @PathVariable UUID productId) {
        return basketService.removeItem(user.userId(), productId);
    }
}
