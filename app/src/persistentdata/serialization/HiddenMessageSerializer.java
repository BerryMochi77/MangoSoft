package persistentdata.serialization;

import java.util.UUID;

/**
 * Stores hidden message ids separately from the base message data so older
 * message rows remain readable.
 */
public class HiddenMessageSerializer implements Serializer<UUID, String[]> {
	@Override
	public String[] serialize(UUID object) {
		return new String[] {object.toString()};
	}

	@Override
	public UUID deserialize(String[] data) {
		return UUID.fromString(data[0]);
	}
}
