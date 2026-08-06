package com.systemdesign.captheorem.cap;

import com.systemdesign.captheorem.cluster.ClusterService;
import com.systemdesign.captheorem.cluster.NodeSnapshot;
import com.systemdesign.captheorem.cap.dto.NodesResponse;
import com.systemdesign.captheorem.cap.dto.PartitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "cluster", description = "CAP theorem simulation — AP views vs CP wallet")
@RestController
public class CapController {

    private final ClusterService cluster;

    public CapController(ClusterService cluster) {
        this.cluster = cluster;
    }

    @GetMapping("/nodes")
    @Operation(summary = "Inspect both cluster nodes", description = "Side-by-side state for nodes A and B plus partition flag.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = NodesResponse.class)))
    public NodesResponse nodes() {
        return new NodesResponse(cluster.isPartitioned(), Arrays.asList(cluster.getNodes()));
    }

    @PostMapping("/admin/partition")
    @Operation(summary = "Toggle network partition simulation")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PartitionResponse.class)))
    public PartitionResponse partition(@Valid @RequestBody PartitionRequest body) {
        cluster.setPartitioned(body.enabled());
        return new PartitionResponse(cluster.isPartitioned());
    }

    @PostMapping("/profile/view")
    @Operation(summary = "Increment product view counter (AP)", description = "Accepts write on node A even during partition. Node B may lag.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ClusterService.ViewIncrementResult.class)))
    public ClusterService.ViewIncrementResult viewProduct() {
        return cluster.incrementProductViews();
    }

    @PostMapping("/wallet/debit")
    @Operation(summary = "Debit wallet balance (CP)", description = "Rejects writes during partition to avoid divergent balances.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ClusterService.DebitResult.class)))
    public ClusterService.DebitResult debit(@Valid @RequestBody DebitRequest body) {
        return cluster.debitWallet(body.amount());
    }

    @PostMapping("/admin/reconcile")
    @Operation(summary = "Heal partition and sync nodes")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ClusterService.ReconcileResult.class)))
    public ClusterService.ReconcileResult reconcile() {
        return cluster.reconcile();
    }
}
