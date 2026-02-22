package com.parser.LLM.Data.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the convert endpoint. Controller receives this after Spring deserializes JSON.
 *
 * WHY a dedicated request DTO (not Map or JsonNode):
 * - Type safety: compiler catches wrong field names and types.
 * - Validation: we can use @NotNull, @Valid so invalid payloads are rejected at the controller.
 * - Clear contract: clients and docs know exactly what to send.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialConvertRequest {

    // WHY @NotNull + message: client must send format; otherwise we don't know how to interpret the payload.
    // Message is returned in 400 response so clients know what to fix.
    @NotNull(message = "Input format is required")
    private InputFormat format;

    /** For RAW_CSV: raw CSV or table text. Optional for other formats. */
    private String rawContent;

    /** For TRANSACTIONS or STATEMENT. WHY @Valid: validate each item (e.g. date, amount required). */
    @Valid
    private List<TransactionItem> transactions;

    /** For PORTFOLIO. */
    @Valid
    private List<HoldingItem> holdings;

    /** Optional context (account, period, balances). Used with STATEMENT. */
    @Valid
    private StatementMetadata metadata;
}
