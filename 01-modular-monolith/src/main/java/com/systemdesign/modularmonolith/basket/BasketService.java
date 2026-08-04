package com.systemdesign.modularmonolith.basket;

import com.systemdesign.modularmonolith.basket.dto.AddItemRequest;
import com.systemdesign.modularmonolith.basket.dto.BasketLine;
import com.systemdesign.modularmonolith.basket.dto.BasketView;
import com.systemdesign.modularmonolith.basket.entity.CartItem;
import com.systemdesign.modularmonolith.basket.repository.CartItemRepository;
import com.systemdesign.modularmonolith.catalog.CatalogService;
import com.systemdesign.modularmonolith.catalog.dto.ProductForOrder;
import com.systemdesign.modularmonolith.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** Mirrors {@code src/modules/basket/basket.service.ts}. */
@Service
public class BasketService {

    private final CartItemRepository cartItems;
    // Basket is only allowed to reach Catalog through its public service -- never through a
    // Product repository. This is the concrete enforcement of "public interfaces, not tables".
    private final CatalogService catalogService;

    public BasketService(CartItemRepository cartItems, CatalogService catalogService) {
        this.cartItems = cartItems;
        this.catalogService = catalogService;
    }

    @Transactional
    public BasketView addItem(UUID userId, AddItemRequest dto) {
        // Validates the product exists (and is priceable) before it's allowed into the basket.
        catalogService.getProductForOrder(dto.productId());

        CartItem existing = cartItems.findByUserIdAndProductId(userId, dto.productId()).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.quantity());
            cartItems.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(dto.productId());
            item.setQuantity(dto.quantity());
            cartItems.save(item);
        }

        return getBasket(userId);
    }

    @Transactional
    public BasketView removeItem(UUID userId, UUID productId) {
        cartItems.deleteByUserIdAndProductId(userId, productId);
        return getBasket(userId);
    }

    public BasketView getBasket(UUID userId) {
        List<CartItem> items = cartItems.findByUserId(userId);

        List<BasketLine> lines = items.stream()
                .map(item -> {
                    ProductForOrder product = catalogService.getProductForOrder(item.getProductId());
                    BigDecimal lineTotal = product.price()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    return new BasketLine(product.id(), product.name(), product.price(), item.getQuantity(), lineTotal);
                })
                .toList();

        BigDecimal total = lines.stream()
                .map(BasketLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new BasketView(userId, lines, total);
    }

    @Transactional
    public void clear(UUID userId) {
        cartItems.deleteByUserId(userId);
    }

    public BasketView assertNotEmpty(UUID userId) {
        BasketView basket = getBasket(userId);
        if (basket.items().isEmpty()) {
            throw new BadRequestException("Basket is empty");
        }
        return basket;
    }
}
