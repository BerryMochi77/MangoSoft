package persistentdata.io;

import java.io.Reader;
import java.io.Writer;

public interface IOFactory {
	public Writer writer(String filename);
	public Reader reader(String filename);
}
