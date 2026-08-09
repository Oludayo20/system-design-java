package com.systemdesign.freshcart.analyticsconsumer.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Inspection endpoint: today's running sales counters. */
    @GetMapping("/stats")
    public SalesStats getStats() {
        return statsService.getStats();
    }
}
