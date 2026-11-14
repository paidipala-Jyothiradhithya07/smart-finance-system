package com.smartfinance.statement.normalize;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateParser {

    private static final Pattern TIME_SUFFIX = Pattern.compile("^(.+?)(?:[T\\s]+\\d{1,2}:\\d{2}.*)$");
    private static final Pattern NUMERIC = Pattern.compile("^(\\d{1,4})[/\\-.](\\d{1,2})[/\\-.](\\d{1,4})$");
    private static final Pattern DAY_MONTH_NAME_YEAR =
            Pattern.compile("^(\\d{1,2})[ /.\\-]+([A-Za-z]{3,9})[ /.,\\-]+(\\d{2}|\\d{4})$");
    private static final Pattern MONTH_NAME_DAY_YEAR =
            Pattern.compile("^([A-Za-z]{3,9})[ /.\\-]+(\\d{1,2})[ ,/.\\-]+(\\d{2}|\\d{4})$");
    private static final String[] MONTHS = {"january", "february", "march", "april", "may", "june", "july",
            "august", "september", "october", "november", "december"};

    private final boolean monthFirst;

    public DateParser(DateOrder order) {
        this.monthFirst = order == DateOrder.MONTH_FIRST;
    }

    /** Looks at every date in the file once and decides whether numbers like 03/04/2026 are day-first or month-first. */
    public static DateOrder detectOrder(Collection<String> rawDates) {
        int dayFirst = 0;
        int monthFirst = 0;
        boolean ambiguous = false;

        for (String raw : rawDates) {
            Matcher m = NUMERIC.matcher(stripTime(raw));
            if (!m.matches()) {
                continue;
            }
            String first = m.group(1);
            String third = m.group(3);
            if (first.length() > 2 || (third.length() != 2 && third.length() != 4)) {
                continue;                                  // year-first (2026-01-05) dates are never ambiguous
            }
            int a = Integer.parseInt(first);
            int b = Integer.parseInt(m.group(2));
            if (a > 12 && b <= 12) {
                dayFirst++;
            } else if (b > 12 && a <= 12) {
                monthFirst++;
            } else if (a <= 12 && b <= 12) {
                ambiguous = true;
            }
        }

        if (dayFirst > 0 && monthFirst > 0) {
            return DateOrder.CONFLICT;
        }
        if (dayFirst > 0) {
            return DateOrder.DAY_FIRST;
        }
        if (monthFirst > 0) {
            return DateOrder.MONTH_FIRST;
        }
        return ambiguous ? DateOrder.AMBIGUOUS : DateOrder.DAY_FIRST;
    }

    public Optional<LocalDate> parse(String raw) {
        String text = stripTime(raw);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            Matcher numeric = NUMERIC.matcher(text);
            if (numeric.matches()) {
                return parseNumeric(numeric.group(1), numeric.group(2), numeric.group(3));
            }
            Matcher dayMonthName = DAY_MONTH_NAME_YEAR.matcher(text);
            if (dayMonthName.matches()) {
                return build(year(dayMonthName.group(3)), monthFromName(dayMonthName.group(2)),
                        Integer.parseInt(dayMonthName.group(1)));
            }
            Matcher monthNameDay = MONTH_NAME_DAY_YEAR.matcher(text);
            if (monthNameDay.matches()) {
                return build(year(monthNameDay.group(3)), monthFromName(monthNameDay.group(1)),
                        Integer.parseInt(monthNameDay.group(2)));
            }
        } catch (DateTimeException | NumberFormatException ex) {
            return Optional.empty();                      // for example 31/02/2026
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseNumeric(String g1, String g2, String g3) {
        if (g1.length() == 4) {                           // yyyy-MM-dd
            if (g3.length() > 2) {
                return Optional.empty();
            }
            return build(Integer.parseInt(g1), Integer.parseInt(g2), Integer.parseInt(g3));
        }
        if (g1.length() > 2) {
            return Optional.empty();
        }
        int year;
        if (g3.length() == 4) {
            year = Integer.parseInt(g3);
        } else if (g3.length() == 2) {
            year = 2000 + Integer.parseInt(g3);
        } else {
            return Optional.empty();
        }
        int a = Integer.parseInt(g1);
        int b = Integer.parseInt(g2);
        return monthFirst ? build(year, a, b) : build(year, b, a);
    }

    private static Optional<LocalDate> build(int year, int month, int day) {
        if (month < 1) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.of(year, month, day));
    }

    private static int year(String text) {
        int value = Integer.parseInt(text);
        return text.length() == 2 ? 2000 + value : value;
    }

    private static int monthFromName(String word) {
        String lower = word.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].startsWith(lower)) {
                return i + 1;
            }
        }
        return 0;
    }

    // "05/01/2026 10:30" and "2026-01-05T10:30:00" both become the date part only
    private static String stripTime(String raw) {
        String text = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        Matcher time = TIME_SUFFIX.matcher(text);
        return time.matches() ? time.group(1).trim() : text;
    }
}
