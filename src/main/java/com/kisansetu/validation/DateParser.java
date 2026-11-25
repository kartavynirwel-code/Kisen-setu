package com.kisansetu.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;

/** Parses the date formats commonly typed in Indian bank sheets. Day-first; impossible dates are rejected. */
public final class DateParser {

    private static final List<DateTimeFormatter> FORMATS = List.of(
            formatter("uuuu-M-d"),
            formatter("d/M/uuuu"),
            formatter("d-M-uuuu"),
            formatter("d.M.uuuu"));

    private DateParser() {
    }

    public static Optional<LocalDate> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        for (DateTimeFormatter format : FORMATS) {
            try {
                return Optional.of(LocalDate.parse(trimmed, format));
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return Optional.empty();
    }

    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }
}
