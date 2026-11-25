package com.kisansetu.upload;

import com.kisansetu.domain.Claim;
import com.kisansetu.domain.ClaimBatch;
import com.kisansetu.domain.Farmer;
import com.kisansetu.domain.KycRecord;
import com.kisansetu.domain.LandRecord;
import com.kisansetu.domain.enums.ClaimStatus;
import com.kisansetu.domain.enums.ClaimType;
import com.kisansetu.domain.enums.KycStatus;
import com.kisansetu.repository.ClaimRepository;
import com.kisansetu.repository.FarmerRepository;
import com.kisansetu.repository.KycRecordRepository;
import com.kisansetu.repository.LandRecordRepository;
import com.kisansetu.security.AadhaarHasher;
import com.kisansetu.security.FieldCipher;
import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.DateParser;
import com.kisansetu.validation.Fields;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Saves rows that passed validation: farmer (Aadhaar and bank account encrypted), land record, KYC and claim.
 * Runs inside the caller's transaction. An existing farmer (same KCC account) is reused and not overwritten.
 */
@Component
@RequiredArgsConstructor
public class ClaimPersister {

    static final String AADHAAR_CONTEXT = "farmer.aadhaar";
    static final String BANK_ACCOUNT_CONTEXT = "farmer.bank_account";

    private final FarmerRepository farmers;
    private final LandRecordRepository landRecords;
    private final KycRecordRepository kycRecords;
    private final ClaimRepository claims;
    private final FieldCipher cipher;
    private final AadhaarHasher hasher;

    public void persist(ClaimBatch batch, List<ClaimRow> validRows) {
        if (validRows.isEmpty()) {
            return;
        }
        Set<String> kccAccounts = validRows.stream().map(r -> r.get(Fields.KCC_ACCOUNT_NO)).collect(Collectors.toSet());
        Map<String, Farmer> byKcc = new HashMap<>();
        farmers.findByKccAccountNoIn(kccAccounts).forEach(f -> byKcc.put(f.getKccAccountNo(), f));

        Set<Long> existingIds = byKcc.values().stream().map(Farmer::getId).collect(Collectors.toSet());
        Set<String> landKeys = new HashSet<>();
        Set<Long> farmersWithKyc = new HashSet<>();
        if (!existingIds.isEmpty()) {
            landRecords.findByFarmer_IdIn(existingIds)
                    .forEach(l -> landKeys.add(l.getFarmer().getId() + "|" + l.getLandRecordId()));
            kycRecords.findByFarmer_IdIn(existingIds).forEach(k -> farmersWithKyc.add(k.getFarmer().getId()));
        }

        for (ClaimRow row : validRows) {
            Farmer farmer = byKcc.computeIfAbsent(row.get(Fields.KCC_ACCOUNT_NO), kcc -> farmers.save(newFarmer(row)));

            if (landKeys.add(farmer.getId() + "|" + row.get(Fields.LAND_RECORD_ID))) {
                landRecords.save(newLandRecord(farmer, row));
            }
            if (farmersWithKyc.add(farmer.getId())) {
                kycRecords.save(newKycRecord(farmer, row));
            }
            claims.save(newClaim(batch, farmer, row));
        }
    }

    private Farmer newFarmer(ClaimRow row) {
        Farmer farmer = new Farmer();
        farmer.setKccAccountNo(row.get(Fields.KCC_ACCOUNT_NO));
        farmer.setFullName(row.get(Fields.FULL_NAME));
        farmer.setFatherName(blankToNull(row.get(Fields.FATHER_NAME)));
        farmer.setDateOfBirth(date(row.get(Fields.DATE_OF_BIRTH)));
        farmer.setMobile(row.get(Fields.MOBILE));
        farmer.setAadhaarEnc(cipher.encrypt(row.get(Fields.AADHAAR), AADHAAR_CONTEXT));
        farmer.setAadhaarHash(hasher.hash(row.get(Fields.AADHAAR)));
        farmer.setBankAccountEnc(cipher.encrypt(row.get(Fields.BANK_ACCOUNT_NO), BANK_ACCOUNT_CONTEXT));
        farmer.setIfsc(row.get(Fields.IFSC));
        farmer.setVillage(blankToNull(row.get(Fields.VILLAGE)));
        farmer.setDistrict(blankToNull(row.get(Fields.DISTRICT)));
        farmer.setState(blankToNull(row.get(Fields.STATE)));
        return farmer;
    }

    private LandRecord newLandRecord(Farmer farmer, ClaimRow row) {
        LandRecord land = new LandRecord();
        land.setFarmer(farmer);
        land.setLandRecordId(row.get(Fields.LAND_RECORD_ID));
        land.setOwnerName(row.get(Fields.LAND_OWNER_NAME));
        land.setAreaHectares(new BigDecimal(row.get(Fields.AREA_HECTARES)));
        land.setVillage(blankToNull(row.get(Fields.VILLAGE)));
        land.setDistrict(blankToNull(row.get(Fields.DISTRICT)));
        land.setState(blankToNull(row.get(Fields.STATE)));
        return land;
    }

    /** The name (and date of birth, when given) already matched the claim during validation. */
    private KycRecord newKycRecord(Farmer farmer, ClaimRow row) {
        KycRecord kyc = new KycRecord();
        kyc.setFarmer(farmer);
        kyc.setKycName(row.get(Fields.KYC_NAME));
        kyc.setKycDob(row.has(Fields.KYC_DOB) ? date(row.get(Fields.KYC_DOB)) : null);
        kyc.setKycStatus(KycStatus.VERIFIED);
        return kyc;
    }

    private Claim newClaim(ClaimBatch batch, Farmer farmer, ClaimRow row) {
        Claim claim = new Claim();
        claim.setBatch(batch);
        claim.setFarmer(farmer);
        claim.setClaimType(ClaimType.valueOf(row.get(Fields.CLAIM_TYPE)));
        claim.setFinancialYear(row.get(Fields.FINANCIAL_YEAR));
        claim.setLoanAmount(new BigDecimal(row.get(Fields.LOAN_AMOUNT)));
        claim.setClaimAmount(new BigDecimal(row.get(Fields.CLAIM_AMOUNT)));
        claim.setStatus(ClaimStatus.PENDING);
        return claim;
    }

    private static LocalDate date(String text) {
        return DateParser.parse(text).orElseThrow(() -> new IllegalStateException("Validated date could not be parsed"));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
