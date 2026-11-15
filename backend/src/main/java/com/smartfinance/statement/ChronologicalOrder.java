package com.smartfinance.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.smartfinance.statement.model.ParsedTransaction;

public final class ChronologicalOrder {

    private ChronologicalOrder() {
    }

    /**
     * Oldest first. Some banks list the newest row first, so the file order is reversed when most
     * dates go downwards. The sort that follows is stable, so rows on the same day keep their real order.
     */
    public static List<ParsedTransaction> sort(List<ParsedTransaction> rows) {
        List<ParsedTransaction> list = new ArrayList<>(rows);
        if (isNewestFirst(list)) {
            Collections.reverse(list);
        }
        list.sort(Comparator.comparing(ParsedTransaction::date));
        return list;
    }

    static boolean isNewestFirst(List<ParsedTransaction> rows) {
        int up = 0;
        int down = 0;
        for (int i = 1; i < rows.size(); i++) {
            int comparison = rows.get(i).date().compareTo(rows.get(i - 1).date());
            if (comparison > 0) {
                up++;
            } else if (comparison < 0) {
                down++;
            }
        }
        return down > up;
    }
}