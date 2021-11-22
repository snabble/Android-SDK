package io.snabble.sdk.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Class to normalize Strings and match them.
 */
public class StringNormalizer {
    private static final Pattern normalizedPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Normalized a string by converting all diacriticals to plain text.
     * <p>
     * This only works with simple diacritical characters (eg. ß will not work).
     *
     * @param s the string to be normalized
     * @return normalized string
     */
    public static String normalize(final String s) {
        final String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalizedPattern.matcher(normalized).replaceAll("").toLowerCase(Locale.getDefault());
    }
}