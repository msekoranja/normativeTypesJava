/**
 * 
 */
package org.epics.nt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.pvaccess.ClientFactory;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.impl.remote.SerializationHelper;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.Structure;

/**
 * @author msekoranja
 *
 */
public class PVTypeBase implements PVType {

	static {
		ClientFactory.start();
	}
	
	private static final long DEFAULT_TIMEOUT = 5;
	private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;
			
	public static final String CHANNEL_PROVIDER_NAME_KEY = "org.epics.nt.channelProviderName";
	
	private static final String chanelProviderName = System.getProperty(CHANNEL_PROVIDER_NAME_KEY, ClientFactory.PROVIDER_NAME);
	private static ChannelProvider channelProvider =
			ChannelProviderRegistryFactory.getChannelProviderRegistry().getProvider(chanelProviderName);
	
	protected final Channel channel;
	protected final ChannelHandler channelHandler = new ChannelHandler(); 
	
	protected final AtomicBoolean isAutoCommit = new AtomicBoolean(false);
	
	private long timeout = DEFAULT_TIMEOUT;
	private TimeUnit unit = DEFAULT_TIMEOUT_UNIT;
	
	class ChannelHandler implements ChannelRequester, ChannelPutRequester
	{
		private final ResettableLatch latch = new ResettableLatch(1);
		private volatile Status lastStatus;		
		private volatile boolean firstConnect = true;
		private volatile ChannelPut channelPut;
		private volatile boolean initializeGet;

		@Override
		public String getRequesterName() {
			return getClass().getName();
		}

		@Override
		public void message(String message, MessageType type) {
			// TODO use Java logging...
			System.err.println("[" + type + "] " + message);
		}

		@Override
		public synchronized void channelCreated(Status status, Channel channel) {
			lastStatus = status;
			if (!status.isSuccess())
				latch.countDown();
		}

		@Override
		public synchronized void channelStateChange(Channel channel, ConnectionState state) {
			if (state == ConnectionState.CONNECTED && firstConnect)
			{
				firstConnect = false;
				channel.createChannelPut(channelHandler, pvRequest);
			}
		}
		
		/**
		 * 
		 * @param timeout
		 * @param unit
		 * @return returns false on timeout or interrupted exception.
		 */
		boolean await(long timeout, TimeUnit unit) {
			try {
				return latch.await(timeout, unit);
			} catch (InterruptedException e) {
				// noop
				return false;
			}
		}

		@Override
		public void channelPutConnect(Status status, ChannelPut channelPut,
				Structure structure) {
			lastStatus = status;
			this.channelPut = channelPut;
			initializeGet = true;
			
			// do initial get
			// TODO configurable initial get?
			channelPut.get();
			// else connectionLatch.countDown();
			
		}

		@Override
		public void getDone(Status status, ChannelPut channelPut,
				PVStructure pvStructure, BitSet bitSet) {
			
			lastStatus = status;
			
			if (lastStatus.isSuccess())
			{
				if (initializeGet)
				{
					initializeGet = false;
					try {
						initialize(pvStructure);
					} catch (Throwable th) {
						lastStatus = StatusFactory.getStatusCreate().
								createStatus(StatusType.ERROR, "failed to initialize", th);
					}
				}
				else
				{
					// local changes where overriden
					changedBitSet.clear();
				}
			}
			
			latch.countDown();
		}

		@Override
		public void putDone(Status status, ChannelPut channelPut) {
			lastStatus = status;
			latch.countDown();
		}
		
		public void get() {
			latch.reset(1);
			channelPut.get();
			
			if (!await(timeout, unit))
				throw new RuntimeException("timeout occured, failed to update '" + channel.getChannelName() + "'");
			
			if (!lastStatus.isSuccess())
				throw new RuntimeException("failed to update '" +  channel.getChannelName() + "':\n" + lastStatus);
		}

		public void put() {
			latch.reset(1);
			channelPut.put(pvStructure, changedBitSet);
			
			if (!await(timeout, unit))
				throw new RuntimeException("timeout occured, failed to commit '" + channel.getChannelName() + "'");
			
			if (!lastStatus.isSuccess())
				throw new RuntimeException("failed to commit '" +  channel.getChannelName() + "':\n" + lastStatus);
		}
		
	}
	
	@Override
	public void update() {
		channelHandler.get();
	}

	@Override
	public void commit() {
		
		// noop check
		if (changedBitSet.cardinality() == 0)
			return;
		
		channelHandler.put();
		changedBitSet.clear();
	}
	
	protected void fieldChanged(PVField field)
	{
		changedBitSet.set(field.getFieldOffset());
		
		if (isAutoCommit.get())
			commit();
	}

	@Override
	public void setAutoCommit(boolean autoCommit) {
		isAutoCommit.set(autoCommit);
	}

	@Override
	public void setTimeout(long timeout, TimeUnit unit) {
		this.timeout = timeout;
		this.unit = unit;
	}

	private class PVTypeMonitorImpl implements MonitorRequester, PVMonitor
	{

		private final ResettableLatch connectionLatch = new ResettableLatch(1);
		private final ResettableLatch monitorLatch = new ResettableLatch(1);
		volatile Status lastStatus;		
		private volatile Monitor monitor;

		@Override
		public String getRequesterName() {
			return getClass().getName();
		}

		@Override
		public void message(String message, MessageType messageType) {
			// delegate
			channelHandler.message(message, messageType);
		}

		@Override
		public void monitorConnect(Status status, Monitor monitor, Structure structure) {
			lastStatus = status;
			this.monitor = monitor;
			connectionLatch.countDown();

			monitor.start();
		}

		/**
		 * @param timeout
		 * @param unit
		 * @return returns false on timeout or interrupted exception.
		 */
		boolean await(long timeout, TimeUnit unit) {
			try {
				return connectionLatch.await(timeout, unit);
			} catch (InterruptedException e) {
				// noop
				return false;
			}
		}

		@Override
		public void monitorEvent(Monitor monitor) {
			monitorLatch.countDown();
		}

		@Override
		public void unlisten(Monitor monitor) {
			// noop
		}

		@Override
		public boolean waitForUpdate() {
			try {
				monitorLatch.await(timeout, unit);
			} catch (InterruptedException e) {
				// noop
				return false;
			}
			
			MonitorElement element;
			while ((element = monitor.poll()) != null)
			{
				SerializationHelper.partialCopy(element.getPVStructure(), pvStructure, element.getChangedBitSet(), false);
				changedBitSet.clear();
				
				monitor.release(element);
				
				// note: we really need to call reset on latch before last poll to avoid race conditions
				monitorLatch.reset(1);
			}

			return true;
		}

		@Override
		public void close() {
			if (monitor != null)
				monitor.destroy();
		}
		
	}
	
	@Override
	public PVMonitor monitor() {
		PVTypeMonitorImpl monitor = new PVTypeMonitorImpl();
		channel.createMonitor(monitor, pvRequest);
		if (!monitor.await(timeout, unit))
			throw new RuntimeException("timeout occured, failed to monitor '" + channel.getChannelName() + "'");
		
		if (!monitor.lastStatus.isSuccess())
			throw new RuntimeException("failed to monitor '" + channel.getChannelName() + "':\n" + monitor.lastStatus);
		
		return monitor;
	}

	
	// TODO configurable
	final PVStructure pvRequest = 
			CreateRequest.create().createRequest("value,timeStamp,alarm,display,control");
	
	protected volatile BitSet changedBitSet;
	protected volatile PVStructure pvStructure;

	public PVTypeBase(String channelName)
	{
		this(channelName, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
	}
	
	public PVTypeBase(String channelName, long timeout, TimeUnit unit)
	{
		setTimeout(timeout, unit);
		channel = channelProvider.createChannel(channelName, channelHandler, ChannelProvider.PRIORITY_DEFAULT);
		if (!channelHandler.await(timeout, unit))
			throw new RuntimeException("timeout occured, failed to connect to '" + channelName + "'");
		
		if (!channelHandler.lastStatus.isSuccess())
			throw new RuntimeException("failed to connect to '" + channelName + "':\n" + channelHandler.lastStatus);

		if (!channel.isConnected() || channelHandler.channelPut == null)
			throw new RuntimeException("failed to connect to '" + channelName + "'");
	}

	protected void initialize(PVStructure pvStructure)
	{
		// noop
		if (this.pvStructure == pvStructure)
			return;
		
		this.pvStructure = pvStructure;
		
		if (changedBitSet == null || changedBitSet.size() < pvStructure.getNumberFields())
			changedBitSet = new BitSet(pvStructure.getNumberFields());
		else
			changedBitSet.clear();
	}
		
	@Override
	public void destroy() {
		channel.destroy();
	}

	@Override
	public Map<String, Object> getMetaData() {
		Map<String, Object> metadata = new HashMap<String, Object>();
		metadata.put("channel", channel);
		metadata.put("isAutoCommit", isAutoCommit.get());
		metadata.put("timeout", timeout);
		metadata.put("timeoutUnit", unit.name());
		metadata.put("changedBitSet", changedBitSet);
		metadata.put("pvStructure", pvStructure);
		return metadata;
	}
	
}
