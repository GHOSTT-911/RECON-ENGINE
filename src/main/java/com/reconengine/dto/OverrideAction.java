package com.reconengine.dto;

/** Manual resolution choices offered in the dashboard's review modal. */
public enum OverrideAction {
    CHARGEBACK_FILED("Manually Resolved: Flagged for Chargeback Dispute"),
    FEE_TOLERANCE_APPROVED("Manually Resolved: Approved Gateway Fee Exception"),
    BANK_ADJUSTMENT_ACCEPTED("Manually Resolved: Accepted Bank Clearing Adjustment");

    private final String resolutionNote;

    OverrideAction(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    /** Lenient parsing: unknown values fall back to a bank clearing adjustment. */
    public static OverrideAction fromString(String value) {
        if (value == null) return BANK_ADJUSTMENT_ACCEPTED;
        try {
            return OverrideAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BANK_ADJUSTMENT_ACCEPTED;
        }
    }
}
