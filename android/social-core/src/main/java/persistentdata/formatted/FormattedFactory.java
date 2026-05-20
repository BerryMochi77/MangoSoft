package persistentdata.formatted;

import java.io.Reader;
import java.io.Writer;

public interface FormattedFactory<S> {
	FormattedWriter<S> writer(Writer documentWriter);
	FormattedReader<S> reader(Reader documentReader);
}
