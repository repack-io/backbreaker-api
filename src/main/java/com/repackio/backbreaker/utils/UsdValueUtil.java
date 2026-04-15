package com.repackio.backbreaker.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing USD value ranges and calculating average values.
 */
@Slf4j
public class UsdValueUtil {

    // Pattern to match value ranges like "$5-$10", "$5 - $10", "$5-10", "5-10", etc.
    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "\\$?\\s*(\\d+(?:\\.\\d+)?)\\s*-\\s*\\$?\\s*(\\d+(?:\\.\\d+)?)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern to match single values like "$5", "5", "$5.00", etc.
    private static final Pattern SINGLE_VALUE_PATTERN = Pattern.compile(
        "\\$?\\s*(\\d+(?:\\.\\d+)?)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse a USD value range string and return the average value.
     * Handles formats like:
     * - "$5-$10" -> 7.50
     * - "$5 - $10" -> 7.50
     * - "5-10" -> 7.50
     * - "$5" -> 5.00
     * - "5" -> 5.00
     *
     * @param valueRange The value range string to parse
     * @return The average value, or null if parsing fails
     */
    public static BigDecimal parseAverageValue(String valueRange) {
        if (valueRange == null || valueRange.isBlank()) {
            return null;
        }

        String trimmed = valueRange.trim();

        try {
            // Try to match range pattern first
            Matcher rangeMatcher = RANGE_PATTERN.matcher(trimmed);
            if (rangeMatcher.find()) {
                BigDecimal low = new BigDecimal(rangeMatcher.group(1));
                BigDecimal high = new BigDecimal(rangeMatcher.group(2));

                // Calculate average and round to 2 decimal places
                return low.add(high)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            }

            // Try to match single value
            Matcher singleMatcher = SINGLE_VALUE_PATTERN.matcher(trimmed);
            if (singleMatcher.find()) {
                return new BigDecimal(singleMatcher.group(1))
                    .setScale(2, RoundingMode.HALF_UP);
            }

            log.warn("Could not parse USD value range: {}", valueRange);
            return null;

        } catch (NumberFormatException e) {
            log.error("Error parsing USD value range: {}", valueRange, e);
            return null;
        }
    }

    /**
     * Format a BigDecimal value as a USD string.
     *
     * @param value The value to format
     * @return Formatted string like "$5.00", or null if value is null
     */
    public static String formatUsd(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return "$" + value.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
