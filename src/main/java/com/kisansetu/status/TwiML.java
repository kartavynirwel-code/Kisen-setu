package com.kisansetu.status;

public final class TwiML {

    private TwiML() {
    }

    /** A TwiML document that makes Twilio reply with {@code text}. The text is XML-escaped. */
    public static String message(String text) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Message>" + escape(text) + "</Message></Response>";
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> {
                    boolean allowed = c == '\n' || c == '\t' || c == '\r' || c >= 0x20;
                    if (allowed) {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
