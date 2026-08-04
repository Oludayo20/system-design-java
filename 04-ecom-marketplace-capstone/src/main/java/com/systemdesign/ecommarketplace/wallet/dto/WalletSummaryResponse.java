package com.systemdesign.ecommarketplace.wallet.dto;

import com.systemdesign.ecommarketplace.wallet.entity.Wallet;
import com.systemdesign.ecommarketplace.wallet.entity.WalletLedgerEntry;
import java.util.List;

/** Mirrors WalletController.me's `{ wallet, ledger }` response shape. */
public record WalletSummaryResponse(Wallet wallet, List<WalletLedgerEntry> ledger) {}
