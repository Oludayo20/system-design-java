package com.systemdesign.freshcart.inventoryconsumer.stock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /** Inspection endpoint: current stock levels, to observe order.placed being applied. */
    @GetMapping("/stock")
    public List<StockItem> getStock() {
        return stockService.getAll();
    }
}
