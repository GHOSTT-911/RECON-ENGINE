package com.reconengine.dto;

import com.reconengine.model.TransactionStatus;

import java.math.BigDecimal;
import java.util.Map;

public record SummaryResponse(
        long totalTransactions,
        Map<TransactionStatus, Long> countsByStatus,
        BigDecimal totalGatewayAmt,
        BigDecimal totalBankAmt,
        BigDecimal netVariance,
        BigDecimal totalMdrFees,
        BigDecimal openExceptionExposure,
        double matchRatePercent) {}
