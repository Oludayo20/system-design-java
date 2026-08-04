package com.systemdesign.asyncqueue.rides.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Direct port of src/rides/dto/create-ride.dto.ts. */
@Getter
@Setter
public class CreateRideDto {

    @Schema(example = "rider-42")
    @NotBlank
    private String riderId;

    @Schema(example = "driver-7")
    @NotBlank
    private String driverId;

    @Schema(example = "24.50", description = "Fare in the local currency, e.g. USD")
    @NotNull
    @Positive
    private BigDecimal fare;

    @Schema(example = "Ikeja, Lagos")
    @NotBlank
    private String pickupLocation;

    @Schema(example = "Lekki, Lagos")
    @NotBlank
    private String dropoffLocation;
}
