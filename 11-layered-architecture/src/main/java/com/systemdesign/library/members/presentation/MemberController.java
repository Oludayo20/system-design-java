package com.systemdesign.library.members.presentation;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.systemdesign.library.members.application.CreateMemberUseCase;
import com.systemdesign.library.members.application.GetMemberUseCase;
import com.systemdesign.library.members.presentation.dto.CreateMemberRequest;
import com.systemdesign.library.members.presentation.dto.MemberResponse;

@Tag(name = "members")
@RestController
@RequestMapping("/members")
public class MemberController {

    private final CreateMemberUseCase createMemberUseCase;
    private final GetMemberUseCase getMemberUseCase;

    public MemberController(CreateMemberUseCase createMemberUseCase, GetMemberUseCase getMemberUseCase) {
        this.createMemberUseCase = createMemberUseCase;
        this.getMemberUseCase = getMemberUseCase;
    }

    @PostMapping
    @Operation(summary = "Register a new library member")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse response = MemberResponse.from(createMemberUseCase.execute(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member by id")
    @ApiResponse(responseCode = "404", description = "No member with this id.")
    public MemberResponse get(@PathVariable UUID id) {
        return MemberResponse.from(getMemberUseCase.execute(id));
    }
}
