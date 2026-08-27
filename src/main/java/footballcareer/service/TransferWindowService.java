package footballcareer.service;

import java.time.LocalDate;

public class TransferWindowService {
    public boolean isOpen(LocalDate date) {
        int month = date.getMonthValue();
        return month == 7 || month == 8 || month == 1;
    }

    public void requireOpen(LocalDate date) {
        if (!isOpen(date)) {
            throw new IllegalStateException("Transfer window is closed.");
        }
    }
}
