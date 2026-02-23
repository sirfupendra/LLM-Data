package com.parser.LLM.Data.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.parser.LLM.Data.utility.Annotations.ValidFinancialRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
@ValidFinancialRequest
public class FinancialConvertRequest {
    private JsonNode jsonInput;

    private List<JsonNode> jsonArray;
}
