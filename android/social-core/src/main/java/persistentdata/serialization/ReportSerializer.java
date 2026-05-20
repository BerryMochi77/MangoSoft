package persistentdata.serialization;

import moderation.Report;

import java.util.UUID;

/**
 * Stores active moderation reports as portable CSV rows:
 * message UUID, reporting user UUID, and report timestamp.
 */
public class ReportSerializer implements Serializer<Report, String[]> {
	@Override
	public String[] serialize(Report object) {
		return new String[] {
				object.message().toString(),
				object.user().toString(),
				String.valueOf(object.timestamp()),
				object.reason()
		};
	}

	@Override
	public Report deserialize(String[] data) {
		return new Report(
				UUID.fromString(data[0]),
				UUID.fromString(data[1]),
				Long.parseLong(data[2]),
				data.length > 3 ? data[3] : ""
		);
	}
}
