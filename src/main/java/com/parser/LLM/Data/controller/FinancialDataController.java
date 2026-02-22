package com.parser.LLM.Data.controller;

import com.parser.LLM.Data.dto.request.FinancialConvertRequest;
import com.parser.LLM.Data.dto.response.FinancialConvertResponse;
import com.parser.LLM.Data.service.FinancialToMarkdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/financial")
@Tag(name = "Financial Data", description = "Convert financial data (transactions, portfolio, CSV, PDFs) to LLM-friendly markdown")
@RequiredArgsConstructor
public class FinancialDataController {

    private final FinancialToMarkdownService financialToMarkdownService;

    @Operation(
            summary = "Convert financial data to markdown",
            description = "Accepts structured JSON (transactions, portfolio, raw CSV text) and converts to LLM-friendly markdown format"
    )
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
            @Valid @RequestBody FinancialConvertRequest request
    ) {
        FinancialConvertResponse response = financialToMarkdownService.convert(request);
        return ResponseEntity.ok(response);
    }

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
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", required = false) String formatHint
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FinancialConvertResponse.builder()
                            .markdown("# Error: File is empty")
                            .format("ERROR")
                            .itemCount(0)
                            .build()
            );
        }

        FinancialConvertResponse response = financialToMarkdownService.convertFile(file, formatHint);
        return ResponseEntity.ok(response);
    }
}
