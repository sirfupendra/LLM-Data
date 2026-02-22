package com.parser.LLM.Data.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItem {

    @NotNull(message = "Transaction date is required")
    private LocalDate date;

    private String description;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String category;
    private String currency;
    private String accountId;
}
