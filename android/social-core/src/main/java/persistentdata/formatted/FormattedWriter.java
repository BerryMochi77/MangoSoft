package persistentdata.formatted;

public interface FormattedWriter<S> {
	/**
	 * Writes the header at the start of the document, prior to any data.
	 */
	void putHeader();

	/**
	 * Formats and writes the next serialized entry.
	 * @param serialized the entry to write
	 */
	void putNext(S serialized);

	/**
	 * Writes the footer at the end of the document, after any data.
	 */
	void putFooter();
}
