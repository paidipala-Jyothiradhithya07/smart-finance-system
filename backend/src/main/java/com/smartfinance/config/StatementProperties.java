package com.smartfinance.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class StatementProperties {
	private int maxFileSizeMb = 5;
    private int maxRows = 5000;
    private int maxFilesPerUpload = 5;
    private int headerScanRows = 30;
    private Map<String, List<String>> aliases = defaultAliases();

    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }

    public int getMaxFilesPerUpload() { return maxFilesPerUpload; }
    public void setMaxFilesPerUpload(int maxFilesPerUpload) { this.maxFilesPerUpload = maxFilesPerUpload; }

    public int getHeaderScanRows() { return headerScanRows; }
    public void setHeaderScanRows(int headerScanRows) { this.headerScanRows = headerScanRows; }

    public Map<String, List<String>> getAliases() { return aliases; }
    public void setAliases(Map<String, List<String>> aliases) { this.aliases = aliases; }

    public long maxFileSizeBytes() {
        return maxFileSizeMb * 1024L * 1024L;
    }

    /**
     * Header names banks use for each column, written the way the mapper normalizes them
     * (lower case, punctuation replaced by single spaces). Earlier entries are preferred.
     */
    public static Map<String, List<String>> defaultAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("date", List.of("transaction date", "txn date", "tran date", "trans date",
                "date", "posting date", "value date", "value dt"));
        map.put("description", List.of("description", "narration", "particulars", "transaction remarks",
                "transaction details", "transaction description", "remarks", "details", "narrative"));
        map.put("debit", List.of("debit", "withdrawal", "withdrawals", "debits", "dr",
                "paid out", "money out", "amount debited"));
        map.put("credit", List.of("credit", "deposit", "deposits", "credits", "cr",
                "paid in", "money in", "amount credited"));
        map.put("amount", List.of("amount", "transaction amount", "txn amount", "withdrawal dr deposit cr"));
        map.put("balance", List.of("balance", "closing balance", "running balance", "available balance",
                "balance after transaction", "ledger balance", "bal", "closing bal"));
        map.put("type", List.of("dr cr", "cr dr", "debit credit", "type", "transaction type",
                "txn type", "dr cr indicator", "cr dr indicator"));
        map.put("reference", List.of("reference no", "ref no", "chq ref no", "cheque number", "cheque no",
                "chq no", "reference number", "reference", "ref", "utr", "transaction id", "txn id"));
        return map;
    }
}
