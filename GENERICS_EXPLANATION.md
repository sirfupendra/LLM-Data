# Why We Use @AllArgsConstructor and @NoArgsConstructor

## Summary
Both annotations are **required** for Jackson (JSON deserialization) and the Builder pattern to work.

### @NoArgsConstructor
- **Why needed:** Jackson needs a no-argument constructor to create objects, then it sets fields via setters.
- **What happens without it:** `Cannot construct instance of FinancialConvertRequest: no Creators, like default constructor, exist`
- **Lombok generates:** `public FinancialConvertRequest() { }`

### @AllArgsConstructor
- **Why needed:** Required for `@Builder` pattern. Builder creates objects via the all-args constructor.
- **Also useful for:** Testing - `new FinancialConvertRequest(format, rawContent, transactions, ...)`
- **Lombok generates:** Constructor with all fields as parameters

## Example
```java
@Data
@Builder
@NoArgsConstructor  // Jackson needs this
@AllArgsConstructor   // Builder needs this
public class TransactionItem {
    private LocalDate date;
    private BigDecimal amount;
}
```

---

# Generics Approach

## Current Design (Non-Generic)
```json
{
  "format": "TRANSACTIONS",
  "transactions": [...],
  "holdings": null,
  "rawContent": null
}
```

**Pros:**
- Simple, straightforward
- Easy to understand
- No Jackson complexity

**Cons:**
- Multiple nullable fields (transactions, holdings, rawContent)
- Client can accidentally send wrong fields for a format

## Generic Design (Alternative)
I've created `FinancialConvertRequestGeneric` and `FinancialDataPayload` as an alternative.

```json
{
  "format": "TRANSACTIONS",
  "data": {
    "transactions": [...]
  }
}
```

**Pros:**
- Single "data" field - cleaner
- Type-safe - compiler enforces correct types
- Extensible - add new formats without new fields

**Cons:**
- More complex Jackson setup (@JsonTypeInfo, @JsonSubTypes)
- Slightly more verbose JSON

**Recommendation:** Start with the non-generic version (current), switch to generic if you need stricter type safety.
