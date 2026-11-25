package com.kisansetu.status;

import java.util.List;

/** Plain-text replies for the SMS/WhatsApp tracker. Kept short and free of personal data other than the KCC number. */
public final class StatusMessages {

    static final int MAX_LENGTH = 1000;
    static final int MAX_REASON = 120;
    private static final String LEGEND = "IS = Interest Subvention, PRI = Prompt Repayment Incentive";

    private StatusMessages() {
    }

    public static String help() {
        return "KisanSetu claim status\nSend: STATUS <your KCC account number>\nExample: STATUS MP2025000101";
    }

    /** Same text whether the KCC account does not exist or belongs to another mobile number. */
    public static String notFound() {
        return "No claim found for this mobile number and KCC account. "
                + "Check the account number or contact your bank branch.";
    }

    public static String tooManyRequests() {
        return "Too many requests. Please try again after some time.";
    }

    public static String status(String kccAccount, List<ClaimStatusLine> lines) {
        if (lines.isEmpty()) {
            return "No claim has been filed yet for KCC " + kccAccount + ".";
        }
        StringBuilder out = new StringBuilder("KCC ").append(kccAccount);
        for (ClaimStatusLine line : lines) {
            String entry = "\n" + line.claimType() + " " + line.financialYear() + ": " + line.status();
            if ("REJECTED".equals(line.status()) && line.rejectionReason() != null && !line.rejectionReason().isBlank()) {
                entry += " - " + shorten(line.rejectionReason().trim(), MAX_REASON);
            }
            if (out.length() + entry.length() > MAX_LENGTH) {
                out.append("\n...more claims not shown. Contact your bank branch.");
                return out.toString();
            }
            out.append(entry);
        }
        if (out.length() + LEGEND.length() + 1 <= MAX_LENGTH) {
            out.append('\n').append(LEGEND);
        }
        return out.toString();
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
