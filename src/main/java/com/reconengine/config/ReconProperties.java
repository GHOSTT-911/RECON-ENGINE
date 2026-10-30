package com.reconengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Business rules, configurable from application.properties (prefix "recon"). */
@ConfigurationProperties(prefix = "recon")
public class ReconProperties {

    /** Merchant Discount Rate charged by the gateway, e.g. 0.02 = 2%. */
    private BigDecimal mdrRate = new BigDecimal("0.02");

    /** Allowed rounding difference (INR) when checking whether a gap equals the MDR fee. */
    private BigDecimal mdrTolerance = new BigDecimal("0.05");

    /** Insert 50 demo transactions when the ledger is empty on startup. */
    private boolean seedSampleData = true;

    public BigDecimal getMdrRate() { return mdrRate; }
    public void setMdrRate(BigDecimal mdrRate) { this.mdrRate = mdrRate; }
    public BigDecimal getMdrTolerance() { return mdrTolerance; }
    public void setMdrTolerance(BigDecimal mdrTolerance) { this.mdrTolerance = mdrTolerance; }
    public boolean isSeedSampleData() { return seedSampleData; }
    public void setSeedSampleData(boolean seedSampleData) { this.seedSampleData = seedSampleData; }
}
