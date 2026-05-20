package moderation;

import dao.model.Message;

import java.util.Iterator;

public class ReportedMessageIteratorFactory {

    public static Iterator<Message> create(
            String strategy,
            int amount,
            ReportRegistry registry
    ) {
        if (strategy == null || amount <= 0) {
            throw new IllegalArgumentException();
        }

        if (strategy.equals("OLDEST")) {
            return new OldestReportedMessageIterator(
                    registry.getReportedMessagesByOldest(),
                    amount
            );
        }

        if (strategy.equals("MOST")) {
            return new MostReportedMessageIterator(
                    registry.getReportedMessagesByMost(),
                    amount
            );
        }

        throw new IllegalArgumentException();
    }
}