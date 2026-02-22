package com.parser.LLM.Data.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One transaction in the request. WHY nested in FinancialConvertRequest: keeps the API contract clear (list of these = transactions).
 * Real-world: banks/cards send date, description, amount, category, currency.
 */
@Data
@Builder
// WHY @NoArgsConstructor: Jackson needs this to deserialize JSON → TransactionItem. Without it: "no Creators, like default constructor".
@NoArgsConstructor
// WHY @AllArgsConstructor: Required for @Builder; also useful for: new TransactionItem(date, desc, amount, ...).
@AllArgsConstructor
public class TransactionItem {

    @NotNull(message = "Transaction date is required")
    private LocalDate date;  // WHY LocalDate: no timezone needed for daily transactions; JSON "2025-02-01" binds automatically.

    private String description;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;  // WHY BigDecimal: money must not use float/double (rounding errors); BigDecimal is exact.

    private String category;
    private String currency;
    private String accountId;
}
