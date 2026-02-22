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

@Service
public class FinancialToMarkdownService {

    public FinancialConvertResponse convert(FinancialConvertRequest request) {
        validateRequest(request);

        return switch (request.getFormat()) {
            case TRANSACTIONS -> buildTransactionResponse(request.getTransactions(), null);
            case PORTFOLIO -> buildPortfolioResponse(request.getHoldings());
            case RAW_CSV -> buildRawCsvResponse(request.getRawContent());
            case STATEMENT -> buildStatementResponse(request.getMetadata(), request.getTransactions());
            case EXCEL -> throw new IllegalArgumentException("EXCEL format is only supported via file upload endpoint (/convert/file), not JSON requests");
        };
    }

    public FinancialConvertResponse convertFile(MultipartFile file, String formatHint) {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        InputFormat detectedFormat = detectFormatFromFile(fileName, contentType, formatHint);

        return switch (detectedFormat) {
            case RAW_CSV -> parseCsvFile(file);
            case STATEMENT -> parsePdfFile(file);
            case EXCEL -> parseExcelFile(file);
            default -> {
                throw new IllegalArgumentException("Unsupported file format: " + contentType);
            }
        };
    }

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
            case EXCEL -> {
                throw new IllegalArgumentException("EXCEL format is only supported via file upload endpoint (/convert/file), not JSON requests");
            }
        }
    }

    private FinancialConvertResponse buildTransactionResponse(List<TransactionItem> transactions, StatementMetadata meta) {
        StringBuilder md = new StringBuilder();

        if (meta != null) {
            appendMetadata(md, meta);
        }

        md.append("## Transactions\n\n");
        md.append("| Date | Description | Amount | Category | Currency | Account |\n");
        md.append("|------|-------------|--------|----------|----------|--------|\n");

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

    private FinancialConvertResponse buildStatementResponse(StatementMetadata meta, List<TransactionItem> transactions) {
        FinancialConvertResponse response = buildTransactionResponse(transactions, meta);
        response.setFormat("STATEMENT");
        return response;
    }

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

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)|\\t", -1);

            String row = List.of(cells).stream()
                    .map(c -> c.replace("\"", "").trim())
                    .map(this::escapePipe)
                    .collect(Collectors.joining(" | "));

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

    private InputFormat detectFormatFromFile(String fileName, String contentType, String formatHint) {
        if (formatHint != null) {
            try {
                return InputFormat.valueOf(formatHint.toUpperCase());
            } catch (IllegalArgumentException e) {
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

        return InputFormat.RAW_CSV;
    }

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

    private FinancialConvertResponse parseCsvFile(MultipartFile file) {
        try {
            String content = new String(file.getBytes());
            return buildRawCsvResponse(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }
    }

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

    private String escapePipe(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ");
    }

    private String formatDecimal(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "";
    }
}
