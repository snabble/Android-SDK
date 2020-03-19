package io.snabble.sdk.utils;

import java.util.Calendar;
import java.util.Date;

public class Age {
    public final int days;
    public final int months;
    public final int years;

    private Age(int days, int months, int years) {
        this.days = days;
        this.months = months;
        this.years = years;
    }

    public static Age calculateAge(Date birthDate) {
        Calendar birthDay = Calendar.getInstance();
        birthDay.setTimeInMillis(birthDate.getTime());

        long currentTime = System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(currentTime);

        int years = now.get(Calendar.YEAR) - birthDay.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int birthMonth = birthDay.get(Calendar.MONTH) + 1;

        int months = currentMonth - birthMonth;

        if (months < 0) {
            years--;
            months = 12 - birthMonth + currentMonth;
            if (now.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
                months--;
            }
        } else if (months == 0 && now.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
            years--;
            months = 11;
        }

        int days;

        if (now.get(Calendar.DATE) > birthDay.get(Calendar.DATE)) {
            days = now.get(Calendar.DATE) - birthDay.get(Calendar.DATE);
        } else if (now.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
            int today = now.get(Calendar.DAY_OF_MONTH);
            now.add(Calendar.MONTH, -1);
            days = now.getActualMaximum(Calendar.DAY_OF_MONTH) - birthDay.get(Calendar.DAY_OF_MONTH) + today;
        } else {
            days = 0;
            if (months == 12) {
                years++;
                months = 0;
            }
        }

        return new Age(days, months, years);
    }
}
