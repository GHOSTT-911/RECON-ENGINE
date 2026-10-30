package com.reconengine.controller;

import com.reconengine.dto.OverrideAction;
import com.reconengine.dto.OverrideRequest;
import com.reconengine.dto.SummaryResponse;
import com.reconengine.dto.UploadResult;
import com.reconengine.model.Transaction;
import com.reconengine.model.TransactionStatus;
import com.reconengine.service.ReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reconcile")
public class ReconciliationController {

    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    /** All transactions, optionally filtered: /transactions?status=UNRESOLVED_EXCEPTION */
    @GetMapping("/transactions")
    public List<Transaction> getTransactions(@RequestParam(required = false) TransactionStatus status) {
        return service.getAllTransactions(status);
    }

    @GetMapping("/transactions/{id}")
    public Transaction getTransaction(@PathVariable String id) {
        return service.getTransaction(id);
    }

    @GetMapping("/summary")
    public SummaryResponse summary() {
        return service.summary();
    }

    /** "Run AI Matcher" button: diagnoses open exceptions. */
    @PostMapping("/run")
    public List<Transaction> runEngine() {
        return service.runMatchingEngine();
    }

    /** Re-applies the matching rules to every non-manual transaction. */
    @PostMapping("/reclassify")
    public List<Transaction> reclassify() {
        return service.reclassifyAll();
    }

    @PostMapping("/override")
    public Transaction applyOverride(@Valid @RequestBody OverrideRequest request) {
        return service.applyOverride(request.id(), OverrideAction.fromString(request.action()));
    }

    @PostMapping("/bulk-approve-mdr")
    public List<Transaction> bulkApproveMdr(@RequestBody List<String> ids) {
        return service.bulkApproveMdr(ids);
    }

    /**
     * CSV upload used by the dashboard. Returns the full ledger (array) so the
     * existing frontend keeps working; import stats are in response headers.
     * Add ?replace=false to append/update instead of replacing the ledger.
     */
    @PostMapping("/upload")
    public ResponseEntity<List<Transaction>> uploadCSV(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "true") boolean replace) {
        UploadResult result = service.importCsv(file, replace);
        return ResponseEntity.ok()
                .header("X-Imported-Count", String.valueOf(result.imported()))
                .header("X-Skipped-Count", String.valueOf(result.skippedRows().size()))
                .body(result.transactions());
    }

    /** Same as /upload but returns import details (imported count, skipped rows). */
    @PostMapping("/upload/details")
    public UploadResult uploadCsvWithDetails(@RequestParam("file") MultipartFile file,
                                             @RequestParam(defaultValue = "true") boolean replace) {
        return service.importCsv(file, replace);
    }

    /** Wipes the ledger and restores the 50 demo transactions. */
    @PostMapping("/reset")
    public List<Transaction> reset() {
        return service.resetToSampleData();
    }
}
