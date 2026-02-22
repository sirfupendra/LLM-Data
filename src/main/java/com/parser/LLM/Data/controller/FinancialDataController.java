package com.parser.LLM.Data.controller;

import com.parser.LLM.Data.dto.request.FinancialConvertRequest;
import com.parser.LLM.Data.dto.response.FinancialConvertResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller layer only: handles HTTP for "convert financial data to markdown".
 * Each annotation and design choice below is commented with WHY we need it and why it's the right choice.
 */
// ----- WHY @RestController -----
// Tells Spring this class handles HTTP requests. Combines @Controller + @ResponseBody.
// @ResponseBody means method return values are written directly to the HTTP body (not a view name).
// Right choice: for APIs we always return JSON body, so @RestController avoids adding @ResponseBody on every method.
@RestController

// ----- WHY @RequestMapping on the class -----
// Defines the base path for ALL endpoints in this controller. Every method URL starts with /api/v1/financial.
// Right choice: versioning (/v1/) lets you change the API later without breaking old clients. Prefix keeps all financial endpoints grouped.
@RequestMapping("/api/v1/financial")
public class FinancialDataController {

    // ----- WHY ResponseEntity<FinancialConvertResponse> (not just FinancialConvertResponse) -----
    // ResponseEntity lets you set status code, headers, and body. For success we use 200 OK; later you might return 201 or 404.
    // Right choice: production APIs need explicit control over status and headers; ResponseEntity is the standard way in Spring.

    // ----- WHY @PostMapping(value = "/convert", consumes = ..., produces = ...) -----
    // value = "/convert" → full path is POST /api/v1/financial/convert.
    // consumes = APPLICATION_JSON_VALUE → we only accept Content-Type: application/json. Other types get 415 Unsupported Media Type.
    // produces = APPLICATION_JSON_VALUE → we only return JSON (Accept header). Makes contract clear and avoids accidental XML/HTML.
    // Right choice: being explicit avoids clients sending form-data or XML and getting unclear errors; also documents the API contract.
    @PostMapping(
            value = "/convert",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FinancialConvertResponse> convertToMarkdown(
            // ----- WHY @Valid -----
            // Triggers Jakarta Bean Validation on the request body. If FinancialConvertRequest has @NotNull, @NotBlank etc.,
            // invalid input is rejected before the method runs, and Spring returns 400 with constraint messages.
            // Right choice: validation at the controller boundary keeps invalid data out of the rest of the app; one place to enforce rules.
            @Valid

            // ----- WHY @RequestBody -----
            // Binds the HTTP request body (JSON) to the Java object. Spring uses Jackson to deserialize JSON into FinancialConvertRequest.
            // Right choice: one object for the whole body gives type safety and validation; no manual JSON parsing.
            @RequestBody
            FinancialConvertRequest request
    ) {
        // Stub response until we add the service layer. Keeps the controller only responsible for HTTP and validation.
        FinancialConvertResponse response = FinancialConvertResponse.builder()
                .markdown("# Placeholder – add conversion logic in service layer next")
                .format(request.getFormat() != null ? request.getFormat().name() : "UNKNOWN")
                .itemCount(0)
                .build();

        // ----- WHY ResponseEntity.ok(response) -----
        // .ok(body) is shorthand for status 200 with body. Clients expect 200 for successful GET/POST that returns data.
        // Right choice: 200 is the standard success code for "here is the result of your request".
        return ResponseEntity.ok(response);
    }
}
