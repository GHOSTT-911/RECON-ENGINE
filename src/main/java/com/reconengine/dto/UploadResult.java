package com.reconengine.dto;

import com.reconengine.model.Transaction;

import java.util.List;

public record UploadResult(int imported, List<String> skippedRows, List<Transaction> transactions) {}
