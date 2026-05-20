package persistentdata.serialization;

import dao.model.Message;

import java.util.UUID;

/**
 * Converts between Messages and String[] by converting each field of
 * Message (UUID, poster, thread, timestamp, body text, hidden) to a string.
 *
 * Edit and delete state is intentionally not serialised here — that state
 * lives in the {@code messagestate} sidecar registries, not on the
 * Message domain object. {@link #deserialize(String[])} still accepts
 * legacy 8-column rows from earlier persisted data and silently ignores
 * the trailing two booleans.
 */
public class MessageSerializer implements Serializer<Message, String[]> {

	@Override
	public String[] serialize(Message object) {
		return new String[] {
				object.id().toString(),
				object.poster().toString(),
				object.thread().toString(),
				String.valueOf(object.timestamp()),
				object.message(),
				String.valueOf(object.isHidden())
		};
	}

	@Override
	public Message deserialize(String[] data) {
		boolean hidden = data.length > 5 && Boolean.parseBoolean(data[5]);
		// Legacy data may also have edited / deleted columns at indices 6
		// and 7. The current model no longer carries that state on Message
		// itself (see messagestate.MessageEditRegistry and
		// MessageDeletionRegistry), so those values are ignored on read.
		return new Message(
				UUID.fromString(data[0]),
				UUID.fromString(data[1]),
				UUID.fromString(data[2]),
				Long.valueOf(data[3]),
				data[4],
				hidden
		);
	}
}
