package com.systemdesign.modularmonolith.basket;

import com.systemdesign.modularmonolith.basket.dto.AddItemRequest;
import com.systemdesign.modularmonolith.basket.dto.BasketView;
import com.systemdesign.modularmonolith.identity.AuthenticatedUser;
import com.systemdesign.modularmonolith.shared.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "basket", description = "Shopping cart — requires Bearer token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/basket")
public class BasketController {

    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    @GetMapping
    @Operation(summary = "Get the current user basket", description = "Prices resolved via CatalogService, not direct SQL joins.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BasketView.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public BasketView getBasket(@CurrentUser AuthenticatedUser user) {
        return basketService.getBasket(user.userId());
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the basket", description = "Merges quantity if the product is already in the cart.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BasketView.class)))
    @ApiResponse(responseCode = "404", description = "Product not found in catalog.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public BasketView addItem(@CurrentUser AuthenticatedUser user, @Valid @RequestBody AddItemRequest dto) {
        return basketService.addItem(user.userId(), dto);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the basket")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BasketView.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public BasketView removeItem(
            @CurrentUser AuthenticatedUser user,
            @Parameter(description = "Product UUID to remove") @PathVariable UUID productId) {
        return basketService.removeItem(user.userId(), productId);
    }
}
