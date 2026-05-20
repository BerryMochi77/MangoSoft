package dao.model;

import java.util.UUID;

/**
 * Simple interface for data classes that hold, and may be referenced by, a unique UUID
 */
public interface HasUUID {
	UUID getUUID();
}
