package com.systemdesign.ecommarketplace.wallet;

import com.systemdesign.ecommarketplace.common.CurrentUser;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.wallet.dto.WalletSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "wallet", description = "Balance and ledger on user shard — requires JWT")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/wallet")
public class WalletController {

  private static final int LEDGER_DEFAULT_LIMIT = 20;

  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @GetMapping("/me")
  @Operation(summary = "Get wallet balance and recent ledger entries", description = "Wallet colocated with User on hash(userId) % 3 shard.")
  @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = WalletSummaryResponse.class)))
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
  public WalletSummaryResponse me(@CurrentUser JwtPayload user) {
    var wallet = walletService.getWallet(user.sub());
    var ledger = walletService.getLedger(user.sub(), LEDGER_DEFAULT_LIMIT);
    return new WalletSummaryResponse(wallet, ledger);
  }
}
