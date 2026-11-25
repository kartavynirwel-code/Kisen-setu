package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.DateParser;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.RowRule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Cross-check: date of birth on the claim vs. date of birth on the KYC record (optional column). */
public class KycDobMatchRule implements RowRule {

    @Override
    public List<FieldError> check(ClaimRow row) {
        if (!row.has(Fields.KYC_DOB)) {
            return List.of();
        }
        Optional<LocalDate> kycDob = DateParser.parse(row.get(Fields.KYC_DOB));
        if (kycDob.isEmpty()) {
            return List.of(new FieldError(row.rowNo(), Fields.KYC_DOB, "DOB_FORMAT",
                    "KYC date of birth is not a valid date", "Use dd/mm/yyyy or yyyy-mm-dd"));
        }
        Optional<LocalDate> dob = DateParser.parse(row.get(Fields.DATE_OF_BIRTH));
        if (dob.isPresent() && !dob.get().equals(kycDob.get())) {
            return List.of(new FieldError(row.rowNo(), Fields.KYC_DOB, "DOB_MISMATCH_KYC",
                    "Date of birth " + dob.get() + " does not match KYC date of birth " + kycDob.get(),
                    "Correct whichever record is wrong"));
        }
        return List.of();
    }
}
