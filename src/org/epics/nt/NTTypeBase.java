/**
 * 
 */
package org.epics.nt;

import java.util.concurrent.TimeUnit;

import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.Control;
import org.epics.pvdata.property.Display;
import org.epics.pvdata.property.PVAlarm;
import org.epics.pvdata.property.PVAlarmFactory;
import org.epics.pvdata.property.PVControl;
import org.epics.pvdata.property.PVControlFactory;
import org.epics.pvdata.property.PVDisplay;
import org.epics.pvdata.property.PVDisplayFactory;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.PVStructure;

/**
 * @author msekoranja
 */
public class NTTypeBase extends PVTypeBase implements NTType {

	protected volatile PVTimeStamp pvTimeStamp;
	protected volatile PVAlarm pvAlarm;
	protected volatile PVDisplay pvDisplay;
	protected volatile PVControl pvControl;

	public NTTypeBase(String channelName) {
		super(channelName);
	}

	public NTTypeBase(String channelName, long timeout, TimeUnit unit) {
		super(channelName, timeout, unit);
	}

	protected void initialize(PVStructure pvStructure)
	{
		super.initialize(pvStructure);

		PVStructure timeStamp = pvStructure.getStructureField("timeStamp");
		if (timeStamp != null)
		{
			if (pvTimeStamp == null)
				pvTimeStamp = PVTimeStampFactory.create();
			pvTimeStamp.attach(timeStamp);
		}
		else if (pvTimeStamp != null)
			pvTimeStamp.detach();
			
		PVStructure alarm = pvStructure.getStructureField("alarm");
		if (alarm != null)
		{
			if (pvAlarm == null)
				pvAlarm = PVAlarmFactory.create();
			pvAlarm.attach(alarm);
		}
		else if (pvAlarm != null)
			pvAlarm.detach();

		PVStructure display = pvStructure.getStructureField("display");
		if (display != null)
		{
			if (pvDisplay == null)
				pvDisplay = PVDisplayFactory.create();
			pvDisplay.attach(display);
		}
		else if (pvDisplay != null)
			pvDisplay.detach();

		PVStructure control = pvStructure.getStructureField("control");
		if (control != null)
		{
			if (pvControl == null)
				pvControl = PVControlFactory.create();
			pvControl.attach(control);
		}
		else if (pvControl != null)
			pvControl.detach();
	}
	
	@Override
	public TimeStamp getTimeStamp() {
		if (pvTimeStamp != null && pvTimeStamp.isAttached())
		{
			TimeStamp val = TimeStampFactory.create();
			pvTimeStamp.get(val);
			return val;
		}
		else
			return null;
	}

	@Override
	public Alarm getAlarm() {
		if (pvAlarm != null && pvAlarm.isAttached())
		{
			Alarm val = new Alarm();
			pvAlarm.get(val);
			return val;
		}
		else
			return null;
	}

	@Override
	public Display getDisplay() {
		if (pvDisplay != null && pvDisplay.isAttached())
		{
			Display val = new Display();
			pvDisplay.get(val);
			return val;
		}
		else
			return null;
	}

	@Override
	public Control getControl() {
		if (pvControl != null && pvControl.isAttached())
		{
			Control val = new Control();
			pvControl.get(val);
			return val;
		}
		else
			return null;
	}
	
}
