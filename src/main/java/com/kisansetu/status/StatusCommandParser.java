package com.kisansetu.status;

import com.kisansetu.validation.Fields;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Understands "STATUS MP2025000101" and just "MP2025000101". Anything else gets the help text. */
public final class StatusCommandParser {

    private static final Pattern KCC = Pattern.compile(Fields.KCC_ACCOUNT_REGEX);

    private StatusCommandParser() {
    }

    public static Optional<String> kccAccount(String body) {
        if (body == null) {
            return Optional.empty();
        }
        String[] words = body.trim().toUpperCase(Locale.ROOT).split("\\s+");
        String candidate = null;
        if (words.length == 2 && words[0].equals("STATUS")) {
            candidate = words[1];
        } else if (words.length == 1 && !words[0].equals("STATUS")) {
            candidate = words[0];
        }
        return candidate != null && KCC.matcher(candidate).matches() && !candidate.equals("HELP")
                ? Optional.of(candidate) : Optional.empty();
    }
}
