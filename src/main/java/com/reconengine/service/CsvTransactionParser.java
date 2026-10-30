package com.reconengine.service;

import com.reconengine.exception.BadRequestException;
import com.reconengine.model.Transaction;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a settlement CSV. Columns are matched by header name (case/space
 * insensitive), so files like the dashboard's own export work too:
 *   id | Transaction ID, utr | UTR, gatewayAmt | Gateway Amount,
 *   bankAmt | Bank Credit
 * If no header is recognised, columns are read by position: id, utr, gateway, bank.
 */
@Component
public class CsvTransactionParser {

    private static final Map<String, String> HEADER_ALIASES = new HashMap<>();

    static {
        for (String h : new String[]{"id", "transactionid", "txnid", "paymentid", "txn"}) HEADER_ALIASES.put(h, "id");
        for (String h : new String[]{"utr", "utrnumber", "reference", "bankreference", "rrn"}) HEADER_ALIASES.put(h, "utr");
        for (String h : new String[]{"gatewayamt", "gatewayamount", "gatewayamountinr", "amount", "gateway", "payoutamount"}) HEADER_ALIASES.put(h, "gateway");
        for (String h : new String[]{"bankamt", "bankamount", "bankcredit", "bankcreditinr", "bank", "settledamount", "creditamount"}) HEADER_ALIASES.put(h, "bank");
    }

    public static class ParseResult {
        public final List<Transaction> transactions;
        public final List<String> skippedRows;

        ParseResult(List<Transaction> transactions, List<String> skippedRows) {
            this.transactions = transactions;
            this.skippedRows = skippedRows;
        }
    }

    public ParseResult parse(InputStream in) {
        List<Transaction> out = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Map<String, Transaction> byId = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            Map<String, Integer> cols = null;
            int autoId = 1000;

            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("﻿")) line = line.substring(1); // strip BOM
                if (line.isBlank()) continue;
                List<String> cells = splitCsvLine(line);

                if (cols == null) {
                    cols = detectHeader(cells);
                    if (cols != null) continue;                // header row consumed
                    cols = Map.of("id", 0, "utr", 1, "gateway", 2, "bank", 3); // no header: positional
                }

                try {
                    String id = cell(cells, cols.get("id"));
                    String utr = cell(cells, cols.get("utr"));
                    String gatewayRaw = cell(cells, cols.get("gateway"));
                    String bankRaw = cell(cells, cols.get("bank"));

                    if (gatewayRaw.isEmpty()) {
                        skipped.add("Line " + lineNo + ": missing gateway amount");
                        continue;
                    }
                    BigDecimal gateway = parseAmount(gatewayRaw);
                    BigDecimal bank = bankRaw.isEmpty() ? gateway : parseAmount(bankRaw);

                    if (id.isEmpty()) id = "pay_CSV" + (autoId++);
                    if (utr.isEmpty()) utr = "UTR_UNKNOWN";

                    Transaction txn = new Transaction();
                    txn.setId(id);
                    txn.setUtr(utr);
                    txn.setGatewayAmt(gateway);
                    txn.setBankAmt(bank);
                    byId.put(id, txn); // later duplicate rows win
                } catch (NumberFormatException e) {
                    skipped.add("Line " + lineNo + ": invalid amount");
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read CSV file: " + e.getMessage());
        }
        out.addAll(byId.values());
        return new ParseResult(out, skipped);
    }

    private static Map<String, Integer> detectHeader(List<String> cells) {
        Map<String, Integer> cols = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            String key = cells.get(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            String canonical = HEADER_ALIASES.get(key);
            if (canonical != null && !cols.containsKey(canonical)) cols.put(canonical, i);
        }
        if (cols.containsKey("gateway")) return cols;
        // Header row that we don't understand (contains no numbers) -> positional
        boolean looksLikeHeader = cells.stream().noneMatch(CsvTransactionParser::isNumber);
        return looksLikeHeader ? Map.of("id", 0, "utr", 1, "gateway", 2, "bank", 3) : null;
    }

    private static boolean isNumber(String s) {
        try { parseAmount(s); return true; } catch (NumberFormatException e) { return false; }
    }

    private static BigDecimal parseAmount(String raw) {
        String cleaned = raw.replace("₹", "").replace("INR", "").replace("Rs.", "")
                .replace(",", "").replace(" ", "").trim();
        if (cleaned.isEmpty()) throw new NumberFormatException("empty");
        return new BigDecimal(cleaned);
    }

    private static String cell(List<String> cells, Integer idx) {
        if (idx == null || idx >= cells.size()) return "";
        return cells.get(idx).trim();
    }

    /** Minimal RFC-4180 splitter: handles quoted fields, commas and "" escapes. */
    static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString());
        return result;
    }
}
