package com.systemdesign.captheorem.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClusterServiceTest {

    @Test
    void acceptsProductViewIncrementsDuringPartition() {
        ClusterService cluster = new ClusterService();
        cluster.setPartitioned(true);

        ClusterService.ViewIncrementResult result = cluster.incrementProductViews();
        NodeSnapshot[] nodes = cluster.getNodes();

        assertTrue(result.accepted());
        assertEquals(1251, nodes[0].productViews());
        assertEquals(1250, nodes[1].productViews());
    }

    @Test
    void rejectsWalletDebitsDuringPartition() {
        ClusterService cluster = new ClusterService();
        cluster.setPartitioned(true);

        ClusterService.DebitResult result = cluster.debitWallet(500);

        assertEquals(
                new ClusterService.DebitResult(
                        false,
                        null,
                        "Partition detected: wallet writes rejected to preserve consistency (CP)"),
                result);
    }
}
