/**
 * 
 */
package org.epics.nt.test;

import org.epics.nt.NTScalarDouble;
import org.epics.nt.PVType.PVMonitor;

/**
 * @author msekoranja
 */
public class NTScalarDoubleExample  {

	@SuppressWarnings("unused")
	public static final void main(String[] args) {

		// a simple scalar channel
		NTScalarDouble adc = new NTScalarDouble("testADC");
		
		// constructor blocks until a channel is connected
		// the object holds a valid (current) value after construction
		
		// get value
		double value = adc.getValue();

		// set value
		adc.setValue(12.3);
		adc.commit();
		
		// to enable auto commit use adc.setAutoCommit(true)
		// to explicitly commit a value when a setter is being called

		// update value
		adc.update();
		value = adc.getValue();
		
		// wait for one monitor event (one change)
		// monitor implements Java7 AutoCloseable
		try (PVMonitor monitor = adc.monitor())
		{
			if (monitor.waitForUpdate())
			{
				value = adc.getValue();
			}
			else
			{
				// timeout occured
				
				// use adc.setTimeout() method or
				// provide timeout at constructon time to
				// set timeout value
			}
		}
	}
	
}
