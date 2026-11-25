package com.kisansetu.validation;

import com.kisansetu.validation.rules.AadhaarRule;
import com.kisansetu.validation.rules.AmountRule;
import com.kisansetu.validation.rules.DateOfBirthRule;
import com.kisansetu.validation.rules.DuplicateRule;
import com.kisansetu.validation.rules.FinancialYearRule;
import com.kisansetu.validation.rules.KycDobMatchRule;
import com.kisansetu.validation.rules.KycNameMatchRule;
import com.kisansetu.validation.rules.LandOwnerMatchRule;
import com.kisansetu.validation.rules.PatternRule;
import com.kisansetu.validation.rules.RequiredFieldsRule;
import java.time.Clock;
import java.util.List;

/** The one place that lists which rules make up the validation engine. Plain Java so it can be used in unit tests. */
public final class ValidationEngineFactory {

    private ValidationEngineFactory() {
    }

    public static ValidationEngine create(Clock clock) {
        List<RowRule> rowRules = List.of(
                new RequiredFieldsRule(),
                new AadhaarRule(),
                new PatternRule(Fields.MOBILE, "MOBILE_FORMAT", "[6-9]\\d{9}",
                        "Mobile must be a 10-digit Indian number starting with 6, 7, 8 or 9",
                        "Remove country code and spaces, then re-check the number"),
                new PatternRule(Fields.IFSC, "IFSC_FORMAT", "[A-Z]{4}0[A-Z0-9]{6}",
                        "IFSC must be 11 characters: 4 letters, then 0, then 6 letters or digits",
                        "Copy the IFSC from the passbook or branch details"),
                new PatternRule(Fields.BANK_ACCOUNT_NO, "BANK_ACCOUNT_FORMAT", "\\d{9,18}",
                        "Bank account number must be 9 to 18 digits",
                        "Copy the account number from the passbook"),
                new PatternRule(Fields.KCC_ACCOUNT_NO, "KCC_ACCOUNT_FORMAT", Fields.KCC_ACCOUNT_REGEX,
                        "KCC account number must be 8 to 20 letters, digits, '/' or '-'",
                        "Copy the KCC account number from the loan record"),
                new PatternRule(Fields.LAND_RECORD_ID, "LAND_RECORD_FORMAT", "[A-Z0-9/-]{4,30}",
                        "Land record ID must be 4 to 30 letters, digits, '/' or '-'",
                        "Copy the land record ID exactly as in the revenue record"),
                new PatternRule(Fields.CLAIM_TYPE, "CLAIM_TYPE_INVALID", "IS|PRI",
                        "Claim type must be IS or PRI",
                        "Use IS for Interest Subvention or PRI for Prompt Repayment Incentive"),
                new DateOfBirthRule(clock),
                new KycDobMatchRule(),
                new FinancialYearRule(),
                new AmountRule(),
                new KycNameMatchRule(),
                new LandOwnerMatchRule());
        return new ValidationEngine(rowRules, List.of(new DuplicateRule()));
    }
}
