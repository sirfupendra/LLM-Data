package com.parser.LLM.Data.controller;

import com.parser.LLM.Data.dto.request.FinancialConvertRequest;
import com.parser.LLM.Data.dto.response.FinancialConvertResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

// ----- WHY @Tag for Swagger -----
// Groups all endpoints in this controller under "Financial Data" in Swagger UI.
// Right choice: makes Swagger docs organized; users see all financial endpoints together.
@Tag(name = "Financial Data", description = "Convert financial data (transactions, portfolio, CSV, PDFs) to LLM-friendly markdown")
public class FinancialDataController {

    // ----- WHY ResponseEntity<FinancialConvertResponse> (not just FinancialConvertResponse) -----
    // ResponseEntity lets you set status code, headers, and body. For success we use 200 OK; later you might return 201 or 404.
    // Right choice: production APIs need explicit control over status and headers; ResponseEntity is the standard way in Spring.

    // ----- WHY @PostMapping(value = "/convert", consumes = ..., produces = ...) -----
    // value = "/convert" → full path is POST /api/v1/financial/convert.
    // consumes = APPLICATION_JSON_VALUE → we only accept Content-Type: application/json. Other types get 415 Unsupported Media Type.
    // produces = APPLICATION_JSON_VALUE → we only return JSON (Accept header). Makes contract clear and avoids accidental XML/HTML.
    // Right choice: being explicit avoids clients sending form-data or XML and getting unclear errors; also documents the API contract.

    // ----- WHY @Operation for Swagger -----
    // Documents the endpoint in Swagger UI: summary, description, what it does.
    // Right choice: auto-generates interactive API docs; developers can test endpoints without Postman/curl.
    @Operation(
            summary = "Convert financial data to markdown",
            description = "Accepts structured JSON (transactions, portfolio, raw CSV text) and converts to LLM-friendly markdown format"
    )
    // ----- WHY @ApiResponses for Swagger -----
    // Documents possible HTTP responses (200, 400, 500) so clients know what to expect.
    // Right choice: complete API contract in Swagger; clients see error formats before coding.
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully converted to markdown",
                    content = @Content(schema = @Schema(implementation = FinancialConvertResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (validation failed)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

    // ----- WHY separate endpoint for file upload -----
    // Files (PDFs, CSVs) come as multipart/form-data, not JSON. Separate endpoint keeps contracts clear.
    // Right choice: one endpoint for JSON, one for files; each has appropriate content-type and validation.
    @Operation(
            summary = "Upload and convert financial file to markdown",
            description = "Accepts PDF, CSV, Excel files and converts to LLM-friendly markdown. Supports: PDF statements, CSV exports, Excel files."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully converted file to markdown",
                    content = @Content(schema = @Schema(implementation = FinancialConvertResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or unsupported format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(
            value = "/convert/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FinancialConvertResponse> convertFileToMarkdown(
            // ----- WHY @RequestParam("file") MultipartFile -----
            // @RequestParam binds the multipart form field named "file" to MultipartFile.
            // MultipartFile gives access to file bytes, name, content-type, size.
            // Right choice: Spring's standard way to handle file uploads; MultipartFile is easy to work with.
            @RequestParam("file") MultipartFile file,

            // ----- WHY optional format parameter -----
            // Client can hint the format (e.g. "STATEMENT" for PDF bank statements), but we can also auto-detect from file extension/content.
            @RequestParam(value = "format", required = false) String formatHint
    ) {
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FinancialConvertResponse.builder()
                            .markdown("# Error: File is empty")
                            .format("ERROR")
                            .itemCount(0)
                            .build()
            );
        }

        // Get file info
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();

        // Stub response - in service layer we'll parse PDF/CSV/Excel and convert to markdown
        String stubMarkdown = String.format(
                "# File uploaded (placeholder)\n\n" +
                "- **Filename:** %s\n" +
                "- **Content-Type:** %s\n" +
                "- **Size:** %d bytes\n" +
                "- **Format hint:** %s\n\n" +
                "*Add file parsing logic in service layer (PDF parsing, CSV parsing, Excel parsing)*",
                fileName, contentType, size, formatHint != null ? formatHint : "auto-detect"
        );

        FinancialConvertResponse response = FinancialConvertResponse.builder()
                .markdown(stubMarkdown)
                .format(formatHint != null ? formatHint : "FILE_UPLOAD")
                .itemCount(0)
                .build();

        return ResponseEntity.ok(response);
    }
}
