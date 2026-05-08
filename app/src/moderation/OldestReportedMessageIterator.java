package moderation;

import dao.model.Message;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class OldestReportedMessageIterator implements Iterator<Message> {

    private final List<Message> messages;
    private final int amount;
    private int index;

    public OldestReportedMessageIterator(List<Message> messages, int amount) {
        this.messages = messages;
        this.amount = amount;
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < messages.size() && index < amount;
    }

    @Override
    public Message next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Message message = messages.get(index);
        index++;
        return message;
    }
}