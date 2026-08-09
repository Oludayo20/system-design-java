package com.systemdesign.orbit.core.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plan catalog (basic/pro/enterprise) and price lookup. No framework imports. */
public final class Plan {

    /** Monthly price in whole currency units (dollars), not cents. */
    public record PlanInfo(String id, String name, double price) {
    }

    /** Ordered lowest to highest — index decides "is this an upgrade or a downgrade?". */
    private static final List<String> PLAN_ORDER = List.of("basic", "pro", "enterprise");

    private static final Map<String, PlanInfo> PLANS = new LinkedHashMap<>();

    static {
        PLANS.put("basic", new PlanInfo("basic", "Basic", 9));
        PLANS.put("pro", new PlanInfo("pro", "Pro", 29));
        PLANS.put("enterprise", new PlanInfo("enterprise", "Enterprise", 99));
    }

    private Plan() {
    }

    public static PlanInfo getPlan(String planId) {
        PlanInfo plan = PLANS.get(planId);
        if (plan == null) {
            throw new UnknownPlanError(String.valueOf(planId));
        }
        return plan;
    }

    public static int planRank(String planId) {
        int rank = PLAN_ORDER.indexOf(planId);
        if (rank == -1) {
            throw new UnknownPlanError(String.valueOf(planId));
        }
        return rank;
    }

    public static List<PlanInfo> listPlans() {
        return PLAN_ORDER.stream().map(PLANS::get).toList();
    }
}
