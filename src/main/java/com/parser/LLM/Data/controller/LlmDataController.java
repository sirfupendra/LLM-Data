package com.parser.LLM.Data.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/llm-data")
public class LlmDataController {

    private static final DateTimeFormatter INPUT_TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final DateTimeFormatter OUTPUT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    /**
     * Accepts raw JSON from market data providers and returns
     * a compact, LLM-friendly representation.
     *
     * Currently supports Alpha Vantage "Time Series" payloads like the example provided.
     */
    @PostMapping("/normalize")
    public ResponseEntity<Map<String, Object>> normalize(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> responseBody = normalizeAlphaVantageTimeSeries(payload);
            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeAlphaVantageTimeSeries(Map<String, Object> payload) {
        Object metaObj = payload.get("Meta Data");
        if (!(metaObj instanceof Map<?, ?> meta)) {
            throw new IllegalArgumentException("Unsupported JSON shape: missing 'Meta Data' object");
        }

        String symbol = stringValue(meta.get("2. Symbol"));
        String intervalRaw = stringValue(meta.get("4. Interval"));
        String timeZone = stringValue(meta.get("6. Time Zone"));

        if (symbol == null || intervalRaw == null || timeZone == null) {
            throw new IllegalArgumentException("Unsupported JSON shape: missing symbol, interval or time zone fields");
        }

        String interval = normalizeInterval(intervalRaw);

        String seriesKey = findTimeSeriesKey(payload);
        if (seriesKey == null) {
            throw new IllegalArgumentException("Unsupported JSON shape: no 'Time Series' key found");
        }

        Object seriesObj = payload.get(seriesKey);
        if (!(seriesObj instanceof Map<?, ?> series)) {
            throw new IllegalArgumentException("Unsupported JSON shape: time series is not an object");
        }

        List<String> timestamps = new ArrayList<>();
        for (Object key : series.keySet()) {
            if (key != null) {
                timestamps.add(key.toString());
            }
        }

        // Sort newest first to roughly match provider output
        timestamps.sort(Comparator.reverseOrder());

        List<List<Object>> dataRows = new ArrayList<>();

        for (String ts : timestamps) {
            Object barObj = series.get(ts);
            if (!(barObj instanceof Map<?, ?> bar)) {
                continue;
            }

            String timeLabel = toTimeLabel(ts);

            Double open = toDouble(bar.get("1. open"));
            Double high = toDouble(bar.get("2. high"));
            Double low = toDouble(bar.get("3. low"));
            Double close = toDouble(bar.get("4. close"));
            Long volume = toLong(bar.get("5. volume"));

            if (open == null || high == null || low == null || close == null || volume == null) {
                // Skip malformed rows instead of failing the whole request
                continue;
            }

            List<Object> row = new ArrayList<>(6);
            row.add(timeLabel);
            row.add(open);
            row.add(high);
            row.add(low);
            row.add(close);
            row.add(volume);

            dataRows.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("s", symbol);
        result.put("i", interval);
        result.put("tz", timeZone);
        result.put("d", dataRows);

        return result;
    }

    private String findTimeSeriesKey(Map<String, Object> payload) {
        for (String key : payload.keySet()) {
            if (key != null && key.startsWith("Time Series")) {
                return key;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String normalizeInterval(String raw) {
        String v = raw.trim();
        String lower = v.toLowerCase(Locale.US);

        if (lower.endsWith("min")) {
            // "5min" -> "5m" (only replace the suffix)
            return lower.substring(0, lower.length() - 3) + "m";
        }

        // Simple fallbacks for some common labels
        if (lower.contains("daily")) {
            return "1d";
        }
        if (lower.contains("weekly")) {
            return "1w";
        }
        if (lower.contains("monthly")) {
            return "1mo";
        }

        // Default: return as-is
        return v;
    }

    private String toTimeLabel(String ts) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(ts, INPUT_TS_FORMAT);
            return dateTime.toLocalTime().format(OUTPUT_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            // Fallback: best-effort extraction of "HH:mm" from flexible time parts
            int spaceIdx = ts.indexOf(' ');
            if (spaceIdx > 0 && spaceIdx + 1 < ts.length()) {
                String timePart = ts.substring(spaceIdx + 1);

                int firstColon = timePart.indexOf(':');
                if (firstColon < 0) {
                    // No colon at all, give back the time part as-is
                    return timePart;
                }

                int secondColon = timePart.indexOf(':', firstColon + 1);
                if (secondColon < 0) {
                    // Format is likely "HH:mm" -> return whole component
                    return timePart;
                } else {
                    // Format is likely "HH:mm:ss" -> trim to "HH:mm"
                    return timePart.substring(0, secondColon);
                }
            }
            return ts;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

