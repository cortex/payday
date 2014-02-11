package se.frikod.payday;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class SIFormat {
    private static String[] suffix = new String[]{"", "k", "m", "b", "t"};

    public static String humanReadable(float number) {
        int unit = 1000;
        if (number < unit) return String.format("%.0f", number);
        int exp = (int) (Math.log(number) / Math.log(unit));
        String pre = String.valueOf("kMGTPE".charAt(exp-1));
        double num = number / Math.pow(unit, exp);
        String format = (num * unit % unit) == 0.0 ? "%.0f %s" : "%.1f %s";
        return String.format(format, num, pre);
    }

    public static String format(double number, int max_length) {
        String r = new DecimalFormat("##0E0").format(number);
        return r;/*
        r = r.replaceAll("E[0-9]", suffix[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);
        while (r.length() > max_length || r.matches("[0-9]+\\.[a-z]")) {
            r = r.substring(0, r.length() - 2) + r.substring(r.length() - 1);
        }
        return r;*/
    }

    public static String formatValue(double value) {
        int power;
        String suffix = " kmbt";
        String formattedNumber = "";

        NumberFormat formatter = new DecimalFormat("#,###.#");
        power = (int)StrictMath.log10(value);
        value = value/(Math.pow(10,(power/3)*3));
        formattedNumber = formatter.format(value);
        formattedNumber = formattedNumber + suffix.charAt(power/3);
        return formattedNumber.length()>4 ?  formattedNumber.replaceAll("\\.[0-9]+", "") : formattedNumber;
    }



    private static char[] c = new char[]{'k', 'm', 'b', 't'};

    /**
     * Recursive implementation, invokes itself for each factor of a thousand, increasing the class on each invokation.
     * @param n the number to format
     * @param iteration in fact this is the class from the array c
     * @return a String representing the number n formatted in a cool looking way.
     */
    public static String coolFormat(double n, int iteration) {
        double d = ((long) n / 100) / 10.0;
        boolean isRound = (d * 10) %10 == 0;//true if the decimal part is equal to 0 (then it's trimmed anyway)
        return (d < 1000? //this determines the class, i.e. 'k', 'm' etc
                ((d > 99.9 || isRound || (!isRound && d > 9.99)? //this decides whether to trim the decimals
                        (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
                ) + "" + c[iteration])
                : coolFormat(d, iteration+1));

    }

}
