package com.systemdesign.orbit.adapters.in.http;

import com.systemdesign.orbit.adapters.in.http.dto.ChangePlanRequest;
import com.systemdesign.orbit.adapters.in.http.dto.ChangePlanResponse;
import com.systemdesign.orbit.adapters.in.http.dto.SubscribeRequest;
import com.systemdesign.orbit.adapters.in.http.dto.SubscriptionResponse;
import com.systemdesign.orbit.core.application.CancelUseCase;
import com.systemdesign.orbit.core.application.ChangePlanUseCase;
import com.systemdesign.orbit.core.application.GetSubscriptionUseCase;
import com.systemdesign.orbit.core.application.SubscribeUseCase;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.in.CancelPort.CancelCommand;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort.ChangePlanCommand;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort.ChangePlanResult;
import com.systemdesign.orbit.core.ports.in.GetSubscriptionPort.GetSubscriptionQuery;
import com.systemdesign.orbit.core.ports.in.SubscribePort.SubscribeCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound/driving adapter #1. This controller calls INTO the core through its input ports (the
 * four use-case classes below) and does nothing else — no business rules live here, only HTTP
 * concerns: request parsing, DTO validation, and response shaping. The CLI adapter
 * (adapters/in/cli/OrbitCliRunner.java) drives the exact same use-case classes.
 */
@Tag(name = "subscriptions")
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscribeUseCase subscribeUseCase;
    private final ChangePlanUseCase changePlanUseCase;
    private final CancelUseCase cancelUseCase;
    private final GetSubscriptionUseCase getSubscriptionUseCase;

    public SubscriptionController(
            SubscribeUseCase subscribeUseCase,
            ChangePlanUseCase changePlanUseCase,
            CancelUseCase cancelUseCase,
            GetSubscriptionUseCase getSubscriptionUseCase) {
        this.subscribeUseCase = subscribeUseCase;
        this.changePlanUseCase = changePlanUseCase;
        this.cancelUseCase = cancelUseCase;
        this.getSubscriptionUseCase = getSubscriptionUseCase;
    }

    @PostMapping
    @Operation(summary = "Subscribe a customer to a plan (charges the plan price immediately)")
    public SubscriptionResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
        Subscription subscription =
                subscribeUseCase.execute(new SubscribeCommand(request.customerId(), request.planId()));
        return SubscriptionResponse.fromDomain(subscription);
    }

    @PostMapping("/{id}/change-plan")
    @Operation(summary = "Upgrade (prorated charge, mid-cycle OK) or attempt a downgrade (rejected mid-cycle)")
    public ChangePlanResponse changePlan(@PathVariable String id, @Valid @RequestBody ChangePlanRequest request) {
        ChangePlanResult result = changePlanUseCase.execute(new ChangePlanCommand(id, request.planId()));
        return ChangePlanResponse.fromResult(result.subscription(), result.proratedAmount());
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Schedule cancellation at period end (does not delete anything now)")
    public SubscriptionResponse cancel(@PathVariable String id) {
        Subscription subscription = cancelUseCase.execute(new CancelCommand(id));
        return SubscriptionResponse.fromDomain(subscription);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a subscription by id")
    public SubscriptionResponse getById(@PathVariable String id) {
        Subscription subscription = getSubscriptionUseCase.execute(new GetSubscriptionQuery(id));
        return SubscriptionResponse.fromDomain(subscription);
    }
}
