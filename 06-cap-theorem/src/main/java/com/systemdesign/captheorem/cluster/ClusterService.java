package com.systemdesign.captheorem.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.stereotype.Service;

@Service
public class ClusterService {

    private boolean partitioned = false;

    private NodeSnapshot nodeA = new NodeSnapshot("A", 1250, 5000);
    private NodeSnapshot nodeB = new NodeSnapshot("B", 1250, 5000);

    public void setPartitioned(boolean value) {
        partitioned = value;
    }

    public boolean isPartitioned() {
        return partitioned;
    }

    public NodeSnapshot[] getNodes() {
        return new NodeSnapshot[] {nodeA, nodeB};
    }

    /**
     * AP choice: accept the write locally even if nodes cannot sync. Reads may disagree briefly
     * (eventual consistency).
     */
    public ViewIncrementResult incrementProductViews() {
        nodeA = new NodeSnapshot(nodeA.name(), nodeA.productViews() + 1, nodeA.walletBalance());

        if (!partitioned) {
            nodeB = new NodeSnapshot(nodeB.name(), nodeA.productViews(), nodeB.walletBalance());
        }

        return new ViewIncrementResult(true, "A", nodeA.productViews());
    }

    /**
     * CP choice: require both nodes to agree before accepting a wallet mutation. During a
     * partition, reject the write instead of risking divergent balances.
     */
    public DebitResult debitWallet(int amount) {
        if (partitioned) {
            return new DebitResult(
                    false,
                    null,
                    "Partition detected: wallet writes rejected to preserve consistency (CP)");
        }

        if (nodeA.walletBalance() < amount) {
            return new DebitResult(false, null, "Insufficient funds");
        }

        int next = nodeA.walletBalance() - amount;
        nodeA = new NodeSnapshot(nodeA.name(), nodeA.productViews(), next);
        nodeB = new NodeSnapshot(nodeB.name(), nodeB.productViews(), next);

        return new DebitResult(true, next, null);
    }

    /** Simulate background replication after a partition heals. */
    public ReconcileResult reconcile() {
        partitioned = false;
        nodeB = new NodeSnapshot(nodeB.name(), nodeA.productViews(), nodeA.walletBalance());

        return new ReconcileResult(nodeA.productViews(), nodeA.walletBalance());
    }

    public record ViewIncrementResult(boolean accepted, String node, int views) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebitResult(boolean accepted, Integer balance, String reason) {}

    public record ReconcileResult(int productViews, int walletBalance) {}
}
