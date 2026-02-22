package com.parser.LLM.Data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for the convert endpoint. Controller returns this; Spring serializes it to JSON.
 *
 * WHY a dedicated response DTO:
 * - Stable contract: clients rely on same field names and types every time.
 * - No accidental exposure: we only send markdown, format, itemCount (not internal entities or DB fields).
 * - Easy to extend later (e.g. add "warnings" or "source" without breaking existing clients if we add optional fields).
 */
@Data
@Builder
// WHY @NoArgsConstructor: Jackson needs this to create response objects during deserialization (if you ever need to deserialize responses).
@NoArgsConstructor
// WHY @AllArgsConstructor: Required for @Builder; also useful for testing: new FinancialConvertResponse(markdown, format, count).
@AllArgsConstructor
public class FinancialConvertResponse {

    /** LLM-friendly markdown. WHY string: simple and portable; LLMs consume text. */
    private String markdown;

    /** Echo of the format we processed. WHY: client can confirm we understood the request. */
    private String format;

    /** Number of items converted. WHY: useful for debugging and UI (e.g. "Converted 42 transactions"). */
    private Integer itemCount;
}
