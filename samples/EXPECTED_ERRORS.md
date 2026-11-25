# Expected result for sample_claims.xlsx / sample_claims.csv

100 data rows -> **85 valid, 15 with errors** (one deliberate error per bad row).
10 more rows are messy but valid (spaces in Aadhaar, +91 in mobile, lowercase IFSC, dd-mm-yyyy dates, 'Rs. 1,50,000' amounts, '2025-26' year, lowercase claim type) and must be auto-fixed, NOT reported.

Row numbers are sheet row numbers (row 1 is the header).

| Row | Expected error code |
|---|---|
| 8 | AADHAAR_CHECKSUM |
| 14 | AADHAAR_FORMAT |
| 22 | MOBILE_FORMAT |
| 29 | MOBILE_FORMAT |
| 35 | IFSC_FORMAT |
| 41 | NAME_MISMATCH_KYC |
| 47 | LAND_OWNER_MISMATCH |
| 53 | DOB_AGE_RANGE |
| 60 | DOB_AGE_RANGE |
| 64 | DUPLICATE_CLAIM |
| 71 | REQUIRED_MISSING |
| 78 | CLAIM_TYPE_INVALID |
| 84 | FINANCIAL_YEAR_INVALID |
| 91 | CLAIM_AMOUNT_EXCEEDS_LOAN |
| 97 | AADHAAR_CONFLICT |
