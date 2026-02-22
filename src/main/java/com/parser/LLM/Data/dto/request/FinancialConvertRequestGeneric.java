package com.parser.LLM.Data.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic version of FinancialConvertRequest using generics.
 *
 * WHY this approach:
 * - Single "data" field instead of separate transactions/holdings/rawContent fields.
 * - Type-safe: compiler knows data type matches format.
 * - Cleaner API: client sends { format: "TRANSACTIONS", data: { transactions: [...] } }.
 *
 * Trade-off: More complex Jackson setup, but better separation of concerns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialConvertRequestGeneric {

    @NotNull(message = "Input format is required")
    private InputFormat format;

    // Generic payload - Jackson deserializes based on format using @JsonTypeInfo
    @Valid
    @NotNull(message = "Data payload is required")
    private FinancialDataPayload data;
}
