package persistentdata.formatted;

import persistentdata.PersistentDataException;

import java.io.IOException;
import java.io.Reader;

public class CSVReader implements FormattedReader<String[]> {
	private final CSVFormat format;
	private final Reader reader;
	private Integer nextChar;

	public CSVReader(CSVFormat format, Reader reader) {
		this.format = format;
		this.reader = reader;
	}

	private boolean eof = false;
	public boolean hasNext() {
		if (eof) return false;
		if (nextChar != null) return true;

		try {
			int character = reader.read();
			if (character == -1) {
				eof = true;
				reader.close();
				return false;
			}
			nextChar = character;
			return true;
		} catch (IOException e) {
			throw new CSVIOException(e.getMessage());
		}
	}

	// These format strings are provided to give you some ideas about what error cases might be encountered,
	// but they aren't complete. If you haven't seen these before, you can fill in the %s with .formatted:
	// for example, "hello %s".formatted("Bernardo") returns "hello Bernardo"
	private static final String LINE_TOO_SHORT_MESSAGE = "Line was too short: expected %s fields but found %s";
	private static final String LINE_TOO_LONG_MESSAGE = "Line was too long: expected %s fields";
	private static final String IMPROPER_ESCAPE_MESSAGE = "EOF reached unexpectedly while escaped";
	private static final String REACHED_EOF_MESSAGE = "Already reached end of file while reading";

	public String[] getNext() {
		if (format.COLUMN_COUNT == 0) {
			throw new CSVIOException(LINE_TOO_LONG_MESSAGE.formatted(0));
		}

		if (!hasNext()) {
			throw new CSVIOException(REACHED_EOF_MESSAGE);
		}

		StringBuilder result = new StringBuilder();
		boolean inSpecialField = false;
		boolean lastWasEscape = false;
		String[] fields = new String[format.COLUMN_COUNT];
		int fieldIndex = 0;
		boolean ignoredFirstQuote = false;
		while (true) {
			int i = readNextCharacter();

			if (i == -1) {
				if (inSpecialField) {
					throw new CSVIOException(IMPROPER_ESCAPE_MESSAGE);
				}
				fields[fieldIndex] = result.toString();
				fieldIndex++;
				if (fieldIndex < format.COLUMN_COUNT) {
					throw new CSVIOException(LINE_TOO_SHORT_MESSAGE.formatted(format.COLUMN_COUNT, fieldIndex));
				}
				break;
			}

			char c = (char) i;
			if (c == format.ESCAPE_MARKER) {
				inSpecialField = !inSpecialField;
				if (result.isEmpty() && !ignoredFirstQuote) {
					ignoredFirstQuote = true;
				} else if (lastWasEscape) {
					result.append(c);
					lastWasEscape = false;
				} else {
					lastWasEscape = true;
				}
			} else {
				lastWasEscape = false;
				if (c == format.FIELD_SEPARATOR && !inSpecialField) {
					fields[fieldIndex] = result.toString();
					fieldIndex++;
					if (fieldIndex >= format.COLUMN_COUNT) {
						throw new CSVIOException(LINE_TOO_LONG_MESSAGE.formatted(format.COLUMN_COUNT));
					}
					ignoredFirstQuote = false;
					result = new StringBuilder();
				} else if (c == format.LINE_SEPARATOR && !inSpecialField) {
					fields[fieldIndex] = result.toString();
					fieldIndex++;
					if (fieldIndex < format.COLUMN_COUNT) {
						throw new CSVIOException(LINE_TOO_SHORT_MESSAGE.formatted(format.COLUMN_COUNT, fieldIndex));
					}
					break;
				} else {
					result.append(c);
				}
			}
		}
		return fields;
	}

	private int readNextCharacter() {
		if (nextChar != null) {
			int character = nextChar;
			nextChar = null;
			return character;
		}

		try {
			int character = reader.read();
			if (character == -1) {
				eof = true;
				reader.close();
			}
			return character;
		} catch (IOException e) {
			throw new CSVIOException(e.getMessage());
		}
	}

	public static class CSVIOException extends PersistentDataException {
		public CSVIOException(String message) {
			super(message);
		}
	}
}
