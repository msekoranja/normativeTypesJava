/**
 * 
 */
package org.epics.nt;

import java.util.concurrent.TimeUnit;

/**
 * @author msekoranja
 */
public class NTScalar extends NTTypeBase {

	public NTScalar(String channelName) {
		super(channelName);
	}

	public NTScalar(String channelName, long timeout, TimeUnit unit) {
		super(channelName, timeout, unit);
	}
}
