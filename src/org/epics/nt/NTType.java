/**
 * 
 */
package org.epics.nt;

import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.Control;
import org.epics.pvdata.property.Display;
import org.epics.pvdata.property.TimeStamp;

/**
 * @author msekoranja
 *
 */
public interface NTType extends PVType {
	TimeStamp getTimeStamp();
	Alarm getAlarm();
	Display getDisplay();
	Control getControl();
}
