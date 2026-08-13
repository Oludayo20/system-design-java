package com.systemdesign.freshcart.inventoryconsumer.stock;

import com.systemdesign.freshcart.inventoryconsumer.rabbitmq.OrderPlacedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Starting catalog. Seeded once on boot so GET /stock has something to show before any order. */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private static final List<StockItem> CATALOG_SEED = List.of(
            new StockItem("milk-1l", "Whole Milk 1L", 100),
            new StockItem("bread-1", "Sliced White Bread", 100),
            new StockItem("eggs-12", "Eggs (12-pack)", 100),
            new StockItem("rice-5kg", "Rice 5kg Bag", 100),
            new StockItem("banana-1kg", "Bananas 1kg", 100));

    private final StockItemRepository stockItems;

    public StockService(StockItemRepository stockItems) {
        this.stockItems = stockItems;
    }

    @PostConstruct
    void seedIfEmpty() {
        if (stockItems.count() > 0) {
            return;
        }
        stockItems.saveAll(CATALOG_SEED);
        log.info("Seeded {} stock items", CATALOG_SEED.size());
    }

    public List<StockItem> getAll() {
        return stockItems.findAllByOrderBySkuAsc();
    }

    /**
     * React to order.placed by decrementing stock for every line item. This has nothing to do
     * with notification-consumer, analytics-consumer, or loyalty-consumer succeeding or failing —
     * each consumer reacts to its own copy of the event independently. If notification-consumer's
     * queue backs up or its process crashes, stock still gets decremented here; there is no
     * cross-consumer ordering requirement between them (see README "Ordering").
     */
    @Transactional
    public void applyOrder(OrderPlacedEvent event) {
        for (OrderPlacedEvent.Item item : event.payload().items()) {
            StockItem stock = stockItems.findById(item.sku())
                    // Unknown SKU: create it on the fly rather than dropping the event, so the
                    // demo never silently loses an order line just because it wasn't seeded.
                    .orElseGet(() -> new StockItem(item.sku(), item.name(), 100));

            int nextQuantity = stock.getQuantity() - item.quantity();
            if (nextQuantity < 0) {
                log.warn("SKU {} would go negative ({} - {}); clamping to 0. A production system "
                        + "would route this to a backorder/compensation flow.",
                        item.sku(), stock.getQuantity(), item.quantity());
            }
            stock.setQuantity(Math.max(0, nextQuantity));
            stockItems.save(stock);

            log.info("order.placed (orderId={}, eventId={}): decremented {} by {} -> {} remaining",
                    event.payload().orderId(), event.eventId(), item.sku(), item.quantity(), stock.getQuantity());
        }
    }
}
