package com.kisansetu.validation;

import java.util.ArrayList;
import java.util.List;

/** Column names of the upload file (lower_snake_case). These are the keys used in {@link ClaimRow}. */
public final class Fields {

    public static final String KCC_ACCOUNT_NO = "kcc_account_no";
    public static final String FULL_NAME = "full_name";
    public static final String FATHER_NAME = "father_name";
    public static final String DATE_OF_BIRTH = "date_of_birth";
    public static final String MOBILE = "mobile";
    public static final String AADHAAR = "aadhaar";
    public static final String KYC_NAME = "kyc_name";
    public static final String KYC_DOB = "kyc_dob";
    public static final String BANK_ACCOUNT_NO = "bank_account_no";
    public static final String IFSC = "ifsc";
    public static final String LAND_RECORD_ID = "land_record_id";
    public static final String LAND_OWNER_NAME = "land_owner_name";
    public static final String AREA_HECTARES = "area_hectares";
    public static final String VILLAGE = "village";
    public static final String DISTRICT = "district";
    public static final String STATE = "state";
    public static final String CLAIM_TYPE = "claim_type";
    public static final String FINANCIAL_YEAR = "financial_year";
    public static final String LOAN_AMOUNT = "loan_amount";
    public static final String CLAIM_AMOUNT = "claim_amount";

    /** Columns that must exist in the file header and must be non-blank in every row. */
    public static final List<String> REQUIRED = List.of(
            KCC_ACCOUNT_NO, FULL_NAME, DATE_OF_BIRTH, MOBILE, AADHAAR, KYC_NAME,
            BANK_ACCOUNT_NO, IFSC, LAND_RECORD_ID, LAND_OWNER_NAME, AREA_HECTARES,
            CLAIM_TYPE, FINANCIAL_YEAR, LOAN_AMOUNT, CLAIM_AMOUNT);

    /** Columns that may be absent from the file. */
    public static final List<String> OPTIONAL = List.of(
            FATHER_NAME, KYC_DOB, VILLAGE, DISTRICT, STATE);

    public static final List<String> ALL = all();

    /** Accepted format of a KCC account number. Banks differ, so this is deliberately generic. */
    public static final String KCC_ACCOUNT_REGEX = "[A-Z0-9/-]{8,20}";

    private static List<String> all() {
        List<String> all = new ArrayList<>(REQUIRED);
        all.addAll(OPTIONAL);
        return List.copyOf(all);
    }

    private Fields() {
    }
}
