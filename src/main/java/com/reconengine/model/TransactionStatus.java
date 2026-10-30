package com.reconengine.model;

public enum TransactionStatus {
    /** Gateway payout and bank credit are identical. */
    EXACT_MATCH,
    /** Difference equals the expected MDR fee (or was approved as such). */
    PARTIAL_MATCH,
    /** Difference that needs a human to look at it. */
    UNRESOLVED_EXCEPTION,
    /** Exception closed by a manual override. */
    RESOLVED_MANUAL
}
