package com.parser.LLM.Data.service;

import com.parser.LLM.Data.dto.request.*;
import com.parser.LLM.Data.dto.response.FinancialConvertResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer: Contains business logic for converting financial data to markdown.
 *
 * WHY separate service layer (not put logic in controller):
 * - Separation of concerns: Controller handles HTTP, Service handles business logic.
 * - Testability: You can test conversion logic without starting Spring/MVC (unit tests).
 * - Reusability: Other controllers or scheduled jobs can call this service.
 * - Single Responsibility: Controller = HTTP, Service = conversion logic.
 *
 * WHY @Service annotation:
 * - Tells Spring this is a service bean (component).
 * - Spring creates one instance and injects it into controllers (dependency injection).
 * - Right choice: Standard Spring pattern; enables dependency injection and testing.
 */
@Service
public class FinancialToMarkdownService {

    /**
     * Converts structured financial data (JSON) to markdown.
     *
     * WHY this method signature:
     * - Takes DTO (FinancialConvertRequest) - clean, typed input.
     * - Returns DTO (FinancialConvertResponse) - clean, typed output.
     * - No HTTP concerns (no HttpServletRequest, no ResponseEntity) - pure business logic.
     * - Right choice: Service doesn't know about HTTP; can be called from anywhere.
     */
    public FinancialConvertResponse convert(FinancialConvertRequest request) {
        // WHY validate here too (not just in controller):
        // Defense in depth - even if controller validation fails, service validates format matches data.
        // Also useful if service is called from non-controller code (scheduled jobs, other services).
        validateRequest(request);

        // WHY switch expression (Java 17):
        // Cleaner than if-else chain; compiler ensures all enum values are handled.
        // Right choice: Type-safe, readable, modern Java pattern.
        return switch (request.getFormat()) {
            case TRANSACTIONS -> buildTransactionResponse(request.getTransactions(), null);
            case PORTFOLIO -> buildPortfolioResponse(request.getHoldings());
            case RAW_CSV -> buildRawCsvResponse(request.getRawContent());
            case STATEMENT -> buildStatementResponse(request.getMetadata(), request.getTransactions());
        };
    }

    /**
     * Converts uploaded file to markdown.
     *
     * WHY separate method for files:
     * - Files need parsing (PDF, CSV, Excel) - different logic than structured JSON.
     * - File handling (MultipartFile) is different from DTOs.
     * - Right choice: Keeps concerns separated; file parsing logic isolated.
     */
    public FinancialConvertResponse convertFile(MultipartFile file, String formatHint) {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        // WHY detect format from file extension/content-type:
        // User might not provide formatHint; we can infer from .pdf, .csv, application/pdf, etc.
        // Right choice: Better UX - auto-detect when possible, use hint as fallback.
        InputFormat detectedFormat = detectFormatFromFile(fileName, contentType, formatHint);

        // WHY switch on format:
        // Each file type needs different parser (PDF parser, CSV parser, Excel parser).
        // Right choice: Clear separation of parsing logic per file type.
        return switch (detectedFormat) {
            case RAW_CSV -> parseCsvFile(file);
            case STATEMENT -> parsePdfFile(file);
            case EXCEL -> parseExcelFile(file);
            default -> {
                throw new IllegalArgumentException("Unsupported file format: " + contentType);
            }
        };
    }

    // ========== VALIDATION METHODS ==========

    /**
     * WHY private validation method:
     * - Keeps validation logic in one place (DRY - Don't Repeat Yourself).
     * - Can be reused if we add more convert methods.
     * - Right choice: Centralized validation, easier to maintain.
     */
    private void validateRequest(FinancialConvertRequest request) {
        switch (request.getFormat()) {
            case RAW_CSV -> {
                if (request.getRawContent() == null || request.getRawContent().isBlank()) {
                    throw new IllegalArgumentException("rawContent is required for format RAW_CSV");
                }
            }
            case TRANSACTIONS -> {
                if (request.getTransactions() == null || request.getTransactions().isEmpty()) {
                    throw new IllegalArgumentException("transactions are required for format TRANSACTIONS");
                }
            }
            case PORTFOLIO -> {
                if (request.getHoldings() == null || request.getHoldings().isEmpty()) {
                    throw new IllegalArgumentException("holdings are required for format PORTFOLIO");
                }
            }
            case STATEMENT -> {
                if (request.getTransactions() == null || request.getTransactions().isEmpty()) {
                    throw new IllegalArgumentException("transactions are required for format STATEMENT");
                }
            }
        }
    }

    // ========== CONVERSION METHODS ==========

    /**
     * WHY StringBuilder for building markdown:
     * - Efficient string concatenation (better than String + String in loops).
     * - Mutable - can append many times without creating new objects.
     * - Right choice: Performance best practice for building strings.
     */
    private FinancialConvertResponse buildTransactionResponse(List<TransactionItem> transactions, StatementMetadata meta) {
        StringBuilder md = new StringBuilder();

        // WHY append metadata first if present:
        // Statement needs account info before transactions; transactions-only doesn't need metadata.
        // Right choice: Conditional logic keeps output clean - only add what's needed.
        if (meta != null) {
            appendMetadata(md, meta);
        }

        md.append("## Transactions\n\n");

        // WHY markdown table format:
        // LLMs understand markdown tables well; easy to parse and reason about.
        // Right choice: Standard format that LLMs are trained on.
        md.append("| Date | Description | Amount | Category | Currency | Account |\n");
        md.append("|------|-------------|--------|----------|----------|--------|\n");

        // WHY enhanced for loop (not stream):
        // Simple iteration - stream would be overkill here. For-each is readable.
        // Right choice: Use streams for complex transformations, loops for simple iteration.
        for (TransactionItem t : transactions) {
            String desc = escapePipe(t.getDescription());
            String cat = escapePipe(t.getCategory());
            String amt = formatDecimal(t.getAmount());
            String curr = t.getCurrency() != null ? t.getCurrency() : "";
            String acc = t.getAccountId() != null ? t.getAccountId() : "";
            md.append("| ")
                    .append(t.getDate()).append(" | ")
                    .append(desc).append(" | ")
                    .append(amt).append(" | ")
                    .append(cat).append(" | ")
                    .append(curr).append(" | ")
                    .append(acc).append(" |\n");
        }

        String formatName = meta != null ? "STATEMENT" : "TRANSACTIONS";
        return FinancialConvertResponse.builder()
                .markdown(md.toString())
                .format(formatName)
                .itemCount(transactions.size())
                .build();
    }

    /**
     * WHY separate method for statement:
     * - Statement = metadata + transactions, but we reuse transaction building logic.
     * - Right choice: DRY principle - don't duplicate transaction table building code.
     */
    private FinancialConvertResponse buildStatementResponse(StatementMetadata meta, List<TransactionItem> transactions) {
        FinancialConvertResponse response = buildTransactionResponse(transactions, meta);
        response.setFormat("STATEMENT");
        return response;
    }

    /**
     * WHY separate method for portfolio:
     * - Different structure than transactions (symbol, quantity, price vs date, description, amount).
     * - Right choice: Each format gets its own conversion method - clear separation.
     */
    private FinancialConvertResponse buildPortfolioResponse(List<HoldingItem> holdings) {
        StringBuilder md = new StringBuilder();
        md.append("## Portfolio / Holdings\n\n");
        md.append("| Symbol | Quantity | Price | Value | Currency |\n");
        md.append("|--------|----------|-------|-------|----------|\n");

        for (HoldingItem h : holdings) {
            String price = h.getPrice() != null ? formatDecimal(h.getPrice()) : "";
            String value = h.getValue() != null ? formatDecimal(h.getValue()) : "";
            String curr = h.getCurrency() != null ? h.getCurrency() : "";
            md.append("| ")
                    .append(h.getSymbol()).append(" | ")
                    .append(formatDecimal(h.getQuantity())).append(" | ")
                    .append(price).append(" | ")
                    .append(value).append(" | ")
                    .append(curr).append(" |\n");
        }

        return FinancialConvertResponse.builder()
                .markdown(md.toString())
                .format("PORTFOLIO")
                .itemCount(holdings.size())
                .build();
    }

    /**
     * WHY parse CSV line by line:
     * - CSV can be large; parsing all at once could cause memory issues.
     * - Line-by-line is memory efficient.
     * - Right choice: Scalable approach for large files.
     */
    private FinancialConvertResponse buildRawCsvResponse(String rawContent) {
        String trimmed = rawContent.strip();
        String[] lines = trimmed.split("\n");

        if (lines.length == 0) {
            return FinancialConvertResponse.builder()
                    .markdown("*No content.*")
                    .format("RAW_CSV")
                    .itemCount(0)
                    .build();
        }

        StringBuilder md = new StringBuilder();
        md.append("## Financial data (from raw CSV/table)\n\n");

        // WHY regex split for CSV:
        // CSV can have quoted fields with commas inside: "Smith, John", 1000
        // Regex handles quoted fields correctly.
        // Right choice: Proper CSV parsing (though for production, consider Apache Commons CSV library).
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)|\\t", -1);

            // WHY stream for cell processing:
            // Transform each cell (trim, remove quotes, escape pipes) - perfect use case for streams.
            // Right choice: Functional style fits data transformation.
            String row = List.of(cells).stream()
                    .map(c -> c.replace("\"", "").trim())
                    .map(this::escapePipe)
                    .collect(Collectors.joining(" | "));

            // WHY first line is header:
            // CSV convention: first line = column names, rest = data rows.
            // Right choice: Standard CSV format that users expect.
            if (i == 0) {
                md.append("| ").append(row).append(" |\n");
                md.append("|").append("---|".repeat(cells.length)).append("\n");
            } else {
                md.append("| ").append(row).append(" |\n");
            }
        }

        return FinancialConvertResponse.builder()
                .markdown(md.toString())
                .format("RAW_CSV")
                .itemCount(Math.max(0, lines.length - 1))
                .build();
    }

    // ========== FILE PARSING METHODS ==========

    /**
     * WHY detect format from file:
     * - User might not provide formatHint; we infer from filename/content-type.
     * - Right choice: Better UX - auto-detect when possible.
     */
    private InputFormat detectFormatFromFile(String fileName, String contentType, String formatHint) {
        if (formatHint != null) {
            try {
                return InputFormat.valueOf(formatHint.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid hint, fall through to auto-detect
            }
        }

        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".pdf")) return InputFormat.STATEMENT;
            if (lower.endsWith(".csv")) return InputFormat.RAW_CSV;
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return InputFormat.EXCEL;
        }

        if (contentType != null) {
            if (contentType.contains("pdf")) return InputFormat.STATEMENT;
            if (contentType.contains("csv")) return InputFormat.RAW_CSV;
            if (contentType.contains("spreadsheet") || contentType.contains("excel")) return InputFormat.EXCEL;
        }

        // WHY default to RAW_CSV:
        // Most common format; safe fallback if we can't detect.
        // Right choice: Fail gracefully - try to process rather than error immediately.
        return InputFormat.RAW_CSV;
    }

    /**
     * PDF parsing using Apache PDFBox: load document, strip text, format as markdown.
     * PDFBox extracts text in reading order; we preserve paragraphs and line breaks for LLM readability.
     */
    private FinancialConvertResponse parsePdfFile(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(document);

            if (rawText == null || rawText.isBlank()) {
                return FinancialConvertResponse.builder()
                        .markdown("# PDF: No extractable text\n\n*Document may be scanned/image-based. OCR not implemented.*")
                        .format("STATEMENT")
                        .itemCount(0)
                        .build();
            }

            StringBuilder md = new StringBuilder();
            md.append("# Financial document (PDF)\n\n");
            md.append("- **Filename:** ").append(file.getOriginalFilename()).append("\n");
            md.append("- **Pages:** ").append(document.getNumberOfPages()).append("\n\n");
            md.append("## Content\n\n");

            String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n").strip();
            String[] paragraphs = normalized.split("\\n\\s*\\n");
            for (String para : paragraphs) {
                String line = para.replace("\n", " ").trim();
                if (!line.isEmpty()) {
                    md.append(line).append("\n\n");
                }
            }

            return FinancialConvertResponse.builder()
                    .markdown(md.toString())
                    .format("STATEMENT")
                    .itemCount(paragraphs.length)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Excel parsing using Apache POI: read all sheets, each sheet as a markdown table.
     * Handles .xlsx (XSSF) and .xls (HSSF) via WorkbookFactory.
     */
    private FinancialConvertResponse parseExcelFile(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            StringBuilder md = new StringBuilder();
            md.append("# Financial data (Excel)\n\n");
            md.append("- **Filename:** ").append(file.getOriginalFilename()).append("\n");
            md.append("- **Sheets:** ").append(workbook.getNumberOfSheets()).append("\n\n");

            int totalRows = 0;
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                md.append("## Sheet: ").append(escapePipe(sheetName)).append("\n\n");

                int maxCols = 0;
                for (Row row : sheet) {
                    if (row.getLastCellNum() > maxCols) {
                        maxCols = (int) row.getLastCellNum();
                    }
                }
                if (maxCols <= 0) {
                    md.append("*Empty sheet*\n\n");
                    continue;
                }

                int rowIndex = 0;
                for (Row row : sheet) {
                    md.append("| ");
                    for (int c = 0; c < maxCols; c++) {
                        Cell cell = row.getCell(c);
                        String value = getCellValueAsString(cell);
                        md.append(escapePipe(value)).append(" | ");
                    }
                    md.append("\n");
                    if (rowIndex == 0) {
                        md.append("|").append("---|".repeat(maxCols)).append("\n");
                    }
                    totalRows++;
                    rowIndex++;
                }
                md.append("\n");
            }

            return FinancialConvertResponse.builder()
                    .markdown(md.toString())
                    .format("EXCEL")
                    .itemCount(totalRows)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        yield cell.getLocalDateTimeCellValue().toString();
                    } catch (Exception e) {
                        yield String.valueOf(cell.getNumericCellValue());
                    }
                }
                yield BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            case BLANK, ERROR -> "";
            default -> cell.toString();
        };
    }

    /**
     * WHY reuse buildRawCsvResponse:
     * - CSV file content is just a string - same as rawContent in JSON request.
     * - Right choice: DRY - reuse existing CSV parsing logic.
     */
    private FinancialConvertResponse parseCsvFile(MultipartFile file) {
        try {
            // WHY read file as string:
            // CSV is text format; read bytes and convert to string.
            // Right choice: Simple approach for text files.
            String content = new String(file.getBytes());
            return buildRawCsvResponse(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * WHY append metadata helper:
     * - Used by both TRANSACTIONS (with meta) and STATEMENT.
     * - Right choice: DRY - don't duplicate metadata formatting code.
     */
    private void appendMetadata(StringBuilder md, StatementMetadata meta) {
        md.append("## Statement\n\n");
        if (meta.getAccountName() != null) md.append("- **Account:** ").append(meta.getAccountName()).append("\n");
        if (meta.getAccountId() != null) md.append("- **Account ID:** ").append(meta.getAccountId()).append("\n");
        if (meta.getPeriodStart() != null) md.append("- **Period start:** ").append(meta.getPeriodStart()).append("\n");
        if (meta.getPeriodEnd() != null) md.append("- **Period end:** ").append(meta.getPeriodEnd()).append("\n");
        if (meta.getOpeningBalance() != null) md.append("- **Opening balance:** ").append(formatDecimal(meta.getOpeningBalance())).append("\n");
        if (meta.getClosingBalance() != null) md.append("- **Closing balance:** ").append(formatDecimal(meta.getClosingBalance())).append("\n");
        if (meta.getCurrency() != null) md.append("- **Currency:** ").append(meta.getCurrency()).append("\n");
        md.append("\n");
    }

    /**
     * WHY escape pipe character:
     * - Markdown tables use | as column separator; if data contains |, it breaks the table.
     * - Escape with \| so markdown renders correctly.
     * - Right choice: Prevents markdown syntax errors in output.
     */
    private String escapePipe(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ");
    }

    /**
     * WHY formatDecimal helper:
     * - BigDecimal.toPlainString() avoids scientific notation (1.23E2 → "123").
     * - Right choice: Human-readable numbers in markdown output.
     */
    private String formatDecimal(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "";
    }
}
