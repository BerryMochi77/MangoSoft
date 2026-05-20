package persistentdata.serialization;

public interface Serializer<T, S> {
	/**
	 * Convert from the object form used in the program's model
	 * to a more primitive representation managed by the FormattedWriter
	 * @param object the object to serialize
	 * @return the serialized data
	 */
	S serialize(T object);

	/**
	 * Convert from the object received from the FormattedReader
	 * to a more abstract representation that can be managed by the application
	 * @param data the serialized data
	 * @return the corresponding object
	 */
	T deserialize(S data);
}
