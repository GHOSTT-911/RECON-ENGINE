package com.reconengine.service;

import com.reconengine.dto.OverrideAction;
import com.reconengine.dto.SummaryResponse;
import com.reconengine.dto.UploadResult;
import com.reconengine.exception.BadRequestException;
import com.reconengine.exception.NotFoundException;
import com.reconengine.model.Transaction;
import com.reconengine.model.TransactionStatus;
import com.reconengine.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {

    private final TransactionRepository repository;
    private final MatchingEngine engine;
    private final CsvTransactionParser csvParser;

    public ReconciliationService(TransactionRepository repository, MatchingEngine engine,
                                 CsvTransactionParser csvParser) {
        this.repository = repository;
        this.engine = engine;
        this.csvParser = csvParser;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions(TransactionStatus status) {
        return status == null ? repository.findAllByOrderByIdAsc() : repository.findByStatusOrderByIdAsc(status);
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    /** "Run AI Matcher": re-diagnoses every open exception. */
    @Transactional
    public List<Transaction> runMatchingEngine() {
        List<Transaction> open = repository.findByStatusOrderByIdAsc(TransactionStatus.UNRESOLVED_EXCEPTION);
        for (Transaction txn : open) {
            txn.setRootCause(engine.diagnose(txn));
        }
        repository.saveAll(open);
        return repository.findAllByOrderByIdAsc();
    }

    /** Re-classifies every transaction from scratch (except manual resolutions). */
    @Transactional
    public List<Transaction> reclassifyAll() {
        List<Transaction> all = repository.findAll();
        for (Transaction txn : all) {
            if (txn.getStatus() != TransactionStatus.RESOLVED_MANUAL) engine.classify(txn);
        }
        repository.saveAll(all);
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional
    public Transaction applyOverride(String txnId, OverrideAction action) {
        Transaction txn = getTransaction(txnId);
        OverrideAction effective = action == null ? OverrideAction.BANK_ADJUSTMENT_ACCEPTED : action;
        txn.setStatus(TransactionStatus.RESOLVED_MANUAL);
        txn.setRootCause(effective.getResolutionNote());
        return repository.save(txn);
    }

    @Transactional
    public List<Transaction> bulkApproveMdr(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) throw new BadRequestException("No transaction ids supplied");
        List<Transaction> list = repository.findAllById(ids);
        for (Transaction txn : list) {
            if (txn.getStatus() == TransactionStatus.UNRESOLVED_EXCEPTION) {
                txn.setStatus(TransactionStatus.PARTIAL_MATCH);
                txn.setRootCause("Bulk Approved: MDR Rate Variance Accepted by Controller");
            }
        }
        repository.saveAll(list);
        return repository.findAllByOrderByIdAsc();
    }

    /** Imports a CSV. replace=true wipes the ledger first (dashboard behaviour). */
    @Transactional
    public UploadResult importCsv(MultipartFile file, boolean replace) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Please choose a non-empty CSV file");
        CsvTransactionParser.ParseResult parsed;
        try {
            parsed = csvParser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file: " + e.getMessage());
        }
        if (parsed.transactions.isEmpty()) {
            throw new BadRequestException("No valid rows found. Expected columns: id, utr, gatewayAmt, bankAmt. "
                    + String.join("; ", parsed.skippedRows));
        }
        parsed.transactions.forEach(engine::classify);
        if (replace) repository.deleteAllInBatch();
        repository.saveAll(parsed.transactions);
        return new UploadResult(parsed.transactions.size(), parsed.skippedRows, repository.findAllByOrderByIdAsc());
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary() {
        List<Transaction> all = repository.findAll();
        Map<TransactionStatus, Long> counts = new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus s : TransactionStatus.values()) counts.put(s, 0L);

        BigDecimal gateway = BigDecimal.ZERO, bank = BigDecimal.ZERO, mdr = BigDecimal.ZERO, openExposure = BigDecimal.ZERO;
        for (Transaction t : all) {
            counts.merge(t.getStatus(), 1L, Long::sum);
            gateway = gateway.add(t.getGatewayAmt());
            bank = bank.add(t.getBankAmt());
            mdr = mdr.add(t.getMdrFee());
            if (t.getStatus() == TransactionStatus.UNRESOLVED_EXCEPTION) {
                openExposure = openExposure.add(t.getVariance().abs());
            }
        }
        long matched = counts.get(TransactionStatus.EXACT_MATCH) + counts.get(TransactionStatus.PARTIAL_MATCH)
                + counts.get(TransactionStatus.RESOLVED_MANUAL);
        double matchRate = all.isEmpty() ? 0 : Math.round(matched * 10000.0 / all.size()) / 100.0;

        return new SummaryResponse(all.size(), counts, MatchingEngine.money(gateway), MatchingEngine.money(bank),
                MatchingEngine.money(gateway.subtract(bank)), MatchingEngine.money(mdr),
                MatchingEngine.money(openExposure), matchRate);
    }

    /** Wipes the ledger and inserts the 50 demo transactions. */
    @Transactional
    public List<Transaction> resetToSampleData() {
        repository.deleteAllInBatch();
        List<Transaction> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            BigDecimal gateway = BigDecimal.valueOf(1000.0 + i * 147.50);
            BigDecimal mdr = engine.calculateMdr(gateway);
            BigDecimal bank;
            if (i % 5 == 0) bank = gateway.subtract(BigDecimal.valueOf(450));   // unexplained shortage
            else if (i % 3 == 0) bank = gateway.subtract(mdr);                  // MDR deducted
            else bank = gateway;                                               // exact
            Transaction t = new Transaction();
            t.setId("pay_RZP" + (100049 + i));
            t.setUtr("UTR" + (889000 + i));
            t.setGatewayAmt(gateway);
            t.setBankAmt(bank);
            data.add(engine.classify(t));
        }
        repository.saveAll(data);
        return repository.findAllByOrderByIdAsc();
    }
}
