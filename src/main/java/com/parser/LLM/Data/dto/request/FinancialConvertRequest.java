package com.parser.LLM.Data.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialConvertRequest {

    @NotNull(message = "Input format is required")
    private InputFormat format;

    private String rawContent;

    @Valid
    private List<TransactionItem> transactions;

    @Valid
    private List<HoldingItem> holdings;

    @Valid
    private StatementMetadata metadata;
}
