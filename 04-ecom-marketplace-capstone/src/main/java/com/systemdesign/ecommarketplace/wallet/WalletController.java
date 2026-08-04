package com.systemdesign.ecommarketplace.wallet;

import com.systemdesign.ecommarketplace.common.CurrentUser;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.wallet.dto.WalletSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors src/modules/wallet/wallet.controller.ts. Protected - requires a valid bearer JWT. */
@Tag(name = "wallet")
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
  public WalletSummaryResponse me(@CurrentUser JwtPayload user) {
    var wallet = walletService.getWallet(user.sub());
    var ledger = walletService.getLedger(user.sub(), LEDGER_DEFAULT_LIMIT);
    return new WalletSummaryResponse(wallet, ledger);
  }
}
