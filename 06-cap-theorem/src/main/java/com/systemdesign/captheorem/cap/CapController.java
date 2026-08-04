package com.systemdesign.captheorem.cap;

import com.systemdesign.captheorem.cluster.ClusterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "cap-demo")
@RestController
public class CapController {

    private final ClusterService cluster;

    public CapController(ClusterService cluster) {
        this.cluster = cluster;
    }

    @GetMapping("/nodes")
    public Map<String, Object> nodes() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("partitioned", cluster.isPartitioned());
        response.put("nodes", cluster.getNodes());
        return response;
    }

    @PostMapping("/admin/partition")
    public Map<String, Boolean> partition(@Valid @RequestBody PartitionRequest body) {
        cluster.setPartitioned(body.enabled());
        return Map.of("partitioned", cluster.isPartitioned());
    }

    @PostMapping("/profile/view")
    public ClusterService.ViewIncrementResult viewProduct() {
        return cluster.incrementProductViews();
    }

    @PostMapping("/wallet/debit")
    public ClusterService.DebitResult debit(@Valid @RequestBody DebitRequest body) {
        return cluster.debitWallet(body.amount());
    }

    @PostMapping("/admin/reconcile")
    public ClusterService.ReconcileResult reconcile() {
        return cluster.reconcile();
    }
}
