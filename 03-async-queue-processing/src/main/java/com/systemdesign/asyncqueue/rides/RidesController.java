package com.systemdesign.asyncqueue.rides;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.systemdesign.asyncqueue.rides.dto.CreateRideDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Direct port of src/rides/rides.controller.ts. */
@Tag(name = "rides")
@RestController
@RequestMapping("/rides")
public class RidesController {

    private final RidesService ridesService;

    public RidesController(RidesService ridesService) {
        this.ridesService = ridesService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Complete a ride (Producer)",
            description = "Persists the ride and publishes ride.completed to RabbitMQ, then returns immediately. "
                    + "The receipt email, analytics record, and loyalty points are produced by independent "
                    + "workers after this request has already responded.")
    @ApiResponse(responseCode = "201", description = "Ride saved, ride.completed published.",
            content = @Content(schema = @Schema(implementation = CompleteRideResult.class)))
    public CompleteRideResult completeRide(@Valid @RequestBody CreateRideDto dto) {
        return ridesService.completeRide(dto);
    }
}
