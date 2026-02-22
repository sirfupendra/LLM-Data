package com.parser.LLM.Data.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One holding in the request. WHY nested: same idea as TransactionItem; list of these = portfolio.
 */
@Data
@Builder
// WHY @NoArgsConstructor: Jackson needs this for JSON deserialization.
@NoArgsConstructor
// WHY @AllArgsConstructor: Required for @Builder pattern.
@AllArgsConstructor
public class HoldingItem {

    @NotBlank(message = "Symbol is required")  // WHY @NotBlank not @NotNull: empty string "" is invalid for symbol; @NotBlank rejects null and "  ".
    private String symbol;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;  // WHY BigDecimal: shares/quantity can be decimal; avoid float for precision.

    private BigDecimal price;
    private BigDecimal value;
    private String currency;
}
