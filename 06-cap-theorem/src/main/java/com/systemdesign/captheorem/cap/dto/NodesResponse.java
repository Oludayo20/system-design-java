package com.systemdesign.captheorem.cap.dto;

import com.systemdesign.captheorem.cluster.NodeSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cluster node state snapshot.")
public record NodesResponse(
        @Schema(example = "false") boolean partitioned,
        List<NodeSnapshot> nodes) {}
