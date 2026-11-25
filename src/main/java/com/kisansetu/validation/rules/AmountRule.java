package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.RowRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sanity checks on land area and money amounts. Scheme-specific caps (e.g. maximum eligible loan)
 * are deliberately not hard-coded here.
 */
public class AmountRule implements RowRule {

    static final BigDecimal MAX_AREA_HECTARES = new BigDecimal("1000");

    @Override
    public List<FieldError> check(ClaimRow row) {
        List<FieldError> errors = new ArrayList<>();

        if (row.has(Fields.AREA_HECTARES)) {
            BigDecimal area = parse(row.get(Fields.AREA_HECTARES));
            if (area == null || area.signum() <= 0 || area.compareTo(MAX_AREA_HECTARES) > 0) {
                errors.add(new FieldError(row.rowNo(), Fields.AREA_HECTARES, "AREA_INVALID",
                        "Land area must be a number greater than 0 and at most " + MAX_AREA_HECTARES + " hectares",
                        "Enter the area in hectares as a plain number"));
            }
        }

        BigDecimal loan = null;
        if (row.has(Fields.LOAN_AMOUNT)) {
            loan = parse(row.get(Fields.LOAN_AMOUNT));
            if (loan == null || loan.signum() <= 0) {
                loan = null;
                errors.add(new FieldError(row.rowNo(), Fields.LOAN_AMOUNT, "LOAN_AMOUNT_INVALID",
                        "Loan amount must be a number greater than 0", "Enter the amount in rupees as a plain number"));
            }
        }

        if (row.has(Fields.CLAIM_AMOUNT)) {
            BigDecimal claim = parse(row.get(Fields.CLAIM_AMOUNT));
            if (claim == null || claim.signum() <= 0) {
                errors.add(new FieldError(row.rowNo(), Fields.CLAIM_AMOUNT, "CLAIM_AMOUNT_INVALID",
                        "Claim amount must be a number greater than 0", "Enter the amount in rupees as a plain number"));
            } else if (loan != null && claim.compareTo(loan) > 0) {
                errors.add(new FieldError(row.rowNo(), Fields.CLAIM_AMOUNT, "CLAIM_AMOUNT_EXCEEDS_LOAN",
                        "Claim amount " + claim.toPlainString() + " is more than the loan amount " + loan.toPlainString(),
                        "Check both amounts against the loan account"));
            }
        }
        return errors;
    }

    private static BigDecimal parse(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
