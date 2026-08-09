package com.systemdesign.library.loans.presentation;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.systemdesign.library.loans.application.BorrowBookUseCase;
import com.systemdesign.library.loans.application.ListMemberLoansUseCase;
import com.systemdesign.library.loans.application.ReturnBookUseCase;
import com.systemdesign.library.loans.presentation.dto.BorrowBookRequest;
import com.systemdesign.library.loans.presentation.dto.LoanResponse;

/**
 * Presentation layer. No class-level {@code @RequestMapping} prefix so the two "members"-rooted
 * and "loans"-rooted routes required by this API can both live on one controller class, matching
 * the endpoint list in the README exactly. Every method below only calls into the Application
 * layer's use cases -- it never reaches into loans.domain or loans.infrastructure directly.
 */
@Tag(name = "loans")
@RestController
public class LoanController {

    private final BorrowBookUseCase borrowBookUseCase;
    private final ReturnBookUseCase returnBookUseCase;
    private final ListMemberLoansUseCase listMemberLoansUseCase;

    public LoanController(
            BorrowBookUseCase borrowBookUseCase,
            ReturnBookUseCase returnBookUseCase,
            ListMemberLoansUseCase listMemberLoansUseCase) {
        this.borrowBookUseCase = borrowBookUseCase;
        this.returnBookUseCase = returnBookUseCase;
        this.listMemberLoansUseCase = listMemberLoansUseCase;
    }

    @PostMapping("/loans")
    @Operation(
            summary = "Borrow a book",
            description = "Applies all 4 borrowing business rules (availability, 3-loan cap, no outstanding "
                    + "overdue loans) via the Domain layer before creating the loan.")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "409", description = "A borrowing rule was violated.")
    @ApiResponse(responseCode = "404", description = "Book or member not found.")
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowBookRequest request) {
        LoanResponse response = LoanResponse.from(borrowBookUseCase.execute(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/loans/{id}/return")
    @Operation(summary = "Return a borrowed book")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "409", description = "Loan was already returned.")
    @ApiResponse(responseCode = "404", description = "Loan not found.")
    public LoanResponse returnLoan(@PathVariable UUID id) {
        return LoanResponse.from(returnBookUseCase.execute(id));
    }

    @GetMapping("/members/{id}/loans")
    @Operation(summary = "List a member's loans (active and historical)")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", description = "No member with this id.")
    public List<LoanResponse> listForMember(@PathVariable UUID id) {
        return listMemberLoansUseCase.execute(id).stream().map(LoanResponse::from).toList();
    }
}
