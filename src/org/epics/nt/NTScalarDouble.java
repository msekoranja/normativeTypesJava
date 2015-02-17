/**
 * 
 */
package org.epics.nt;

import java.util.concurrent.TimeUnit;

import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVStructure;

/**
 * @author msekoranja
 *
 */
public class NTScalarDouble extends NTScalar {

	protected volatile PVDouble pvValue;
	
	public NTScalarDouble(String channelName) {
		super(channelName);
	}

	public NTScalarDouble(String channelName, long timeout, TimeUnit unit) {
		super(channelName, timeout, unit);
	}
	
	@Override
	protected void initialize(PVStructure pvStructure) {
		super.initialize(pvStructure);
		pvValue = pvStructure.getDoubleField("value");
		if (pvValue == null)
			throw new RuntimeException("incompatible data type");
	}

	public double getValue()
	{
		return pvValue.get();
	}
	
	public void setValue(double value)
	{
		pvValue.put(value);
		fieldChanged(pvValue);
	}
	
}
