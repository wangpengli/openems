package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.openems.edge.common.component.OpenemsComponent;

public class ShortWriteChannel extends ShortReadChannel implements WriteChannel<Short> {

	public static class MirrorToDebugChannel implements Consumer<Channel<Short>> {

		private final ChannelId targetChannelId;

		public MirrorToDebugChannel(ChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<Short> channel) {
			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((ShortWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	}

	protected ShortWriteChannel(OpenemsComponent component, ChannelId channelId, ShortDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Short> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Short value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Short> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<Consumer<Short>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(Consumer<Short> callback) {
		this.getOnSetNextWrites().add(callback);
	}

}
