package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.DateParser;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.RowRule;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

public class DateOfBirthRule implements RowRule {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 100;

    private final Clock clock;

    public DateOfBirthRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<FieldError> check(ClaimRow row) {
        String text = row.get(Fields.DATE_OF_BIRTH);
        if (text.isBlank()) {
            return List.of();
        }
        Optional<LocalDate> dob = DateParser.parse(text);
        if (dob.isEmpty()) {
            return List.of(error(row, "DOB_FORMAT", "Date of birth is not a valid date",
                    "Use dd/mm/yyyy or yyyy-mm-dd"));
        }
        LocalDate today = LocalDate.now(clock);
        if (dob.get().isAfter(today)) {
            return List.of(error(row, "DOB_AGE_RANGE", "Date of birth is in the future",
                    "Check the year of birth"));
        }
        int age = Period.between(dob.get(), today).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            return List.of(error(row, "DOB_AGE_RANGE",
                    "Age " + age + " is outside the expected range " + MIN_AGE + "-" + MAX_AGE,
                    "Check the date of birth against the KYC document"));
        }
        return List.of();
    }

    private static FieldError error(ClaimRow row, String code, String message, String suggestion) {
        return new FieldError(row.rowNo(), Fields.DATE_OF_BIRTH, code, message, suggestion);
    }
}
