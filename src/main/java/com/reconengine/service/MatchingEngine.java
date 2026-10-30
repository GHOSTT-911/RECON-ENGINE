package com.reconengine.service;

import com.reconengine.config.ReconProperties;
import com.reconengine.model.Transaction;
import com.reconengine.model.TransactionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure reconciliation rules: compares the gateway payout with the bank credit
 * and decides the status plus a human-readable root cause.
 */
@Component
public class MatchingEngine {

    private final ReconProperties properties;

    public MatchingEngine(ReconProperties properties) {
        this.properties = properties;
    }

    public BigDecimal calculateMdr(BigDecimal gatewayAmt) {
        return money(gatewayAmt.multiply(properties.getMdrRate()));
    }

    /** Sets mdrFee, status and rootCause on the transaction from its two amounts. */
    public Transaction classify(Transaction txn) {
        BigDecimal gateway = money(txn.getGatewayAmt());
        BigDecimal bank = money(txn.getBankAmt());
        BigDecimal mdr = calculateMdr(gateway);
        BigDecimal diff = gateway.subtract(bank).abs();

        txn.setGatewayAmt(gateway);
        txn.setBankAmt(bank);
        txn.setMdrFee(mdr);

        if (diff.signum() == 0) {
            txn.setStatus(TransactionStatus.EXACT_MATCH);
            txn.setRootCause("Auto-matched: UTR, Gateway Payout & Bank Credit Verified");
        } else if (diff.subtract(mdr).abs().compareTo(properties.getMdrTolerance()) <= 0) {
            txn.setStatus(TransactionStatus.PARTIAL_MATCH);
            txn.setRootCause("Verified: Net Settlement Variance equals Standard "
                    + percent(properties.getMdrRate()) + " MDR Rate");
        } else {
            txn.setStatus(TransactionStatus.UNRESOLVED_EXCEPTION);
            String direction = bank.compareTo(gateway) < 0 ? "Payout Shortage" : "Excess Bank Credit";
            txn.setRootCause(direction + ": Unresolved ₹" + diff.toPlainString()
                    + " discrepancy between Gateway and Bank Credit");
        }
        return txn;
    }

    /** Deeper diagnosis used by the "Run AI Matcher" button for open exceptions. */
    public String diagnose(Transaction txn) {
        BigDecimal gap = money(txn.getGatewayAmt().subtract(txn.getBankAmt()));
        BigDecimal mdr = txn.getMdrFee() == null ? calculateMdr(txn.getGatewayAmt()) : txn.getMdrFee();

        if (gap.signum() < 0) {
            return "Automated Java Engine: Bank credited ₹" + gap.abs().toPlainString()
                    + " more than gateway payout (Duplicate Credit / Reversal Pending)";
        }
        BigDecimal beyondMdr = money(gap.subtract(mdr));
        if (beyondMdr.signum() > 0) {
            return "Automated Java Engine: Detected ₹" + gap.toPlainString()
                    + " hold, of which ₹" + beyondMdr.toPlainString()
                    + " exceeds MDR (Chargeback Reserve / Processing Delay)";
        }
        return "Automated Java Engine: Detected ₹" + gap.toPlainString()
                + " hold (Chargeback Reserve / Processing Delay)";
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String percent(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
