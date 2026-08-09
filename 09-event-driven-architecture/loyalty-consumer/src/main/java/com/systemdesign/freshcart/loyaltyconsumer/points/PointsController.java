package com.systemdesign.freshcart.loyaltyconsumer.points;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    /**
     * Inspection endpoint: points balance per customer, plus how many distinct eventIds have
     * been applied (useful for confirming the duplicate-delivery demo didn't double-count).
     */
    @GetMapping("/points")
    public PointsSummary getSummary() {
        return pointsService.getSummary();
    }
}
