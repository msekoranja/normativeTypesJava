/**
 * 
 */
package org.epics.nt;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author msekoranja
 *
 */
public interface PVType {
	
	void update();
	void commit();

	public interface PVMonitor extends AutoCloseable {
		boolean waitForUpdate();
		void close();
	}
	
	PVMonitor monitor();
	
	Map<String, Object> getMetaData();

	void destroy();
	
	// --- config
	
	void setAutoCommit(boolean autoCommit);
	void setTimeout(long timeout, TimeUnit unit);
	
}
