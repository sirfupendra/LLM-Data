package com.parser.LLM.Data.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Optional context for statements: account name, period, balances.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementMetadata {

    private String accountName;
    private String accountId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private String currency;
}
