package com.systemdesign.ecommarketplace.wallet.dto;

import com.systemdesign.ecommarketplace.wallet.entity.Wallet;
import com.systemdesign.ecommarketplace.wallet.entity.WalletLedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Wallet balance and recent ledger entries.")
public record WalletSummaryResponse(Wallet wallet, List<WalletLedgerEntry> ledger) {}
