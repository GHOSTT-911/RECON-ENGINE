package com.reconengine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 64, nullable = false)
    private String utr;

    @Column(name = "gateway_amt", precision = 14, scale = 2, nullable = false)
    private BigDecimal gatewayAmt;

    @Column(name = "bank_amt", precision = 14, scale = 2, nullable = false)
    private BigDecimal bankAmt;

    @Column(name = "mdr_fee", precision = 14, scale = 2, nullable = false)
    private BigDecimal mdrFee;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private TransactionStatus status;

    @Column(name = "root_cause", length = 1000)
    private String rootCause;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Transaction() {}

    public Transaction(String id, String utr, BigDecimal gatewayAmt, BigDecimal bankAmt,
                       BigDecimal mdrFee, TransactionStatus status, String rootCause) {
        this.id = id;
        this.utr = utr;
        this.gatewayAmt = gatewayAmt;
        this.bankAmt = bankAmt;
        this.mdrFee = mdrFee;
        this.status = status;
        this.rootCause = rootCause;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /** Gateway amount minus bank credit (positive = bank paid less). */
    public BigDecimal getVariance() {
        if (gatewayAmt == null || bankAmt == null) return BigDecimal.ZERO;
        return gatewayAmt.subtract(bankAmt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }
    public BigDecimal getGatewayAmt() { return gatewayAmt; }
    public void setGatewayAmt(BigDecimal gatewayAmt) { this.gatewayAmt = gatewayAmt; }
    public BigDecimal getBankAmt() { return bankAmt; }
    public void setBankAmt(BigDecimal bankAmt) { this.bankAmt = bankAmt; }
    public BigDecimal getMdrFee() { return mdrFee; }
    public void setMdrFee(BigDecimal mdrFee) { this.mdrFee = mdrFee; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public Instant getUpdatedAt() { return updatedAt; }
}
