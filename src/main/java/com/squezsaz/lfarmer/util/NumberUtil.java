package com.squezsaz.lfarmer.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberUtil {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.US));

    private NumberUtil() {
    }

    public static String format(double value) {
        return FORMAT.format(value);
    }

    public static String format(long value) {
        return FORMAT.format(value);
    }

    public static String formatCompact(long count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        char formatChar = "kMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f%c", count / Math.pow(1000, exp), formatChar);
    }

    public static String formatCompact(double count) {
        if (count < 1000) return format(count);
        int exp = (int) (Math.log(count) / Math.log(1000));
        char formatChar = "kMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f%c", count / Math.pow(1000, exp), formatChar);
    }
}
