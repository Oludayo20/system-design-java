package com.systemdesign.faas.triggers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.systemdesign.faas.runtime.ExecutionEnvironmentManager;
import com.systemdesign.faas.runtime.FunctionStatsSnapshot;

@Tag(name = "runtime", description = "Emulator introspection")
@RestController
public class RuntimeStatsController {

    private final ExecutionEnvironmentManager manager;

    public RuntimeStatsController(ExecutionEnvironmentManager manager) {
        this.manager = manager;
    }

    @Operation(summary = "Per-function invocation counts, cold/warm split, and total billed ms")
    @GetMapping("/_runtime/stats")
    public Map<String, FunctionStatsSnapshot> stats() {
        return manager.getStats();
    }
}
