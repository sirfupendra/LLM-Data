package com.parser.LLM.Data.dto.request;

/**
 * WHY enum for format: client can only send one of these values. Invalid value → 400 (Jackson fails to deserialize).
 * Keeps validation and docs in one place.
 */
public enum InputFormat {

    /** List of transactions (bank, card, bookkeeping). */
    TRANSACTIONS,

    /** Portfolio/holdings (symbol, quantity, price). */
    PORTFOLIO,

    /** Raw CSV or table text pasted from Excel/bank export. */
    RAW_CSV,

    /** Statement with metadata + transactions (e.g. full bank statement). */
    STATEMENT
}
