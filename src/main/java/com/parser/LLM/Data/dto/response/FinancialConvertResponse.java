package com.parser.LLM.Data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialConvertResponse {

    private String markdown;
    private String format;
    private Integer itemCount;
}
