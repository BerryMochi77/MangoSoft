package persistentdata.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ComputerIOFactory implements IOFactory {
	private static final String FULL_FILENAME_TEMPLATE = "saved/%s.txt";
	private static final Path SAVE_DIRECTORY = Path.of("saved");

	private static String parseFullFilename(String file) {
		return FULL_FILENAME_TEMPLATE.formatted(file);
	}

	@Override
	public Writer writer(String filename) {
		try {
			Files.createDirectories(SAVE_DIRECTORY);
			return new FileWriter(parseFullFilename(filename));
		} catch (IOException ignored) {
			return null;
		}
	}

	@Override
	public Reader reader(String filename) {
		try {
			return new FileReader(parseFullFilename(filename));
		} catch (IOException ignored) {
			return null;
		}
	}
}
