package net.rafaelinfante.subscriptions.domain.enums;

import java.time.LocalDate;

public enum BillingInterval {
    MONTH {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusMonths(1);
        }
    },
    YEAR {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusYears(1);
        }
    };

    public abstract LocalDate advance(LocalDate from);
}
