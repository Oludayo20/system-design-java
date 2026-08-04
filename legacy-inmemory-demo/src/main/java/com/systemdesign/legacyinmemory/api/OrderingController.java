package com.systemdesign.legacyinmemory.api;

import com.systemdesign.legacyinmemory.modules.ordering.Order;
import com.systemdesign.legacyinmemory.modules.ordering.OrderingService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Java port of the {@code /orders/:userId} route in {@code api/server.js}. */
@RestController
@RequestMapping("/orders")
public class OrderingController {

    private final OrderingService orderingService;

    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> placeOrder(@PathVariable int userId) {
        Order order = orderingService.placeOrder(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("order", order);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
