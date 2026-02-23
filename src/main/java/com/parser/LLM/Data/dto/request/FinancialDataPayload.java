package com.parser.LLM.Data.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic payload wrapper using generics to handle different data types.
 *
 * WHY generics instead of separate fields:
 * - Type safety: compiler enforces that TRANSACTIONS uses List<TransactionItem>, PORTFOLIO uses List<HoldingItem>, etc.
 * - Single source of truth: one field "data" instead of transactions/holdings/rawContent scattered.
 * - Extensible: add new formats without adding new fields to FinancialConvertRequest.
 *
 * HOW it works:
 * - @JsonTypeInfo tells Jackson to include a "type" field in JSON to know which subclass to deserialize.
 * - @JsonSubTypes maps each format enum value to its corresponding payload class.
 * - Jackson reads the "format" from parent and deserializes "data" into the right type.
 */
@Data

@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "format", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TransactionPayload.class, name = "TRANSACTIONS"),
    @JsonSubTypes.Type(value = PortfolioPayload.class, name = "PORTFOLIO"),
    @JsonSubTypes.Type(value = RawCsvPayload.class, name = "RAW_CSV"),
    @JsonSubTypes.Type(value = StatementPayload.class, name = "STATEMENT")
})
public abstract class FinancialDataPayload {
    // Base class for all payload types - Jackson uses this for polymorphic deserialization
}

// ----- TRANSACTIONS payload -----
@Data
@NoArgsConstructor
@AllArgsConstructor
class TransactionPayload extends FinancialDataPayload {
    private List<TransactionItem> transactions;
}

// ----- PORTFOLIO payload -----
@Data
@NoArgsConstructor
@AllArgsConstructor
class PortfolioPayload extends FinancialDataPayload {
    private List<HoldingItem> holdings;
}

// ----- RAW_CSV payload -----
@Data
@NoArgsConstructor
@AllArgsConstructor
class RawCsvPayload extends FinancialDataPayload {
    private String rawContent;
}

// ----- STATEMENT payload -----
@Data
@NoArgsConstructor
@AllArgsConstructor
class StatementPayload extends FinancialDataPayload {
    private StatementMetadata metadata;
    private List<TransactionItem> transactions;
}
