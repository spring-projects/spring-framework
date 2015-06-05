package org.springframework.rx.io;

import java.io.IOException;
import java.io.OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.rx.util.BlockingByteBufQueue;
import org.springframework.rx.util.BlockingByteBufQueuePublisher;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class ByteBufPublisherOutputStream extends OutputStream {

	private final BlockingByteBufQueue queue = new BlockingByteBufQueue();

	private final ByteBufAllocator bufferAllocator;

	public ByteBufPublisherOutputStream() {
		this(new UnpooledByteBufAllocator(false));
	}

	public ByteBufPublisherOutputStream(ByteBufAllocator bufferAllocator) {
		Assert.notNull(bufferAllocator, "'bufferAllocator' must not be null");
		this.bufferAllocator = bufferAllocator;
	}

	public Publisher<ByteBuf> toByteBufPublisher() {
		return new BlockingByteBufQueuePublisher(this.queue);
	}

	@Override
	public void write(int b) throws IOException {
		ByteBuf buffer = this.bufferAllocator.buffer(1, 1);
		buffer.writeByte(b);
		putBuffer(buffer);
	}

	@Override
	public void write(byte[] b) throws IOException {
		ByteBuf buffer = this.bufferAllocator.buffer(b.length, b.length);
		buffer.writeBytes(b);
		putBuffer(buffer);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ByteBuf buffer = this.bufferAllocator.buffer(len, len);
		buffer.writeBytes(b, off, len);
		putBuffer(buffer);
	}

	private void putBuffer(ByteBuf buffer) {
		try {
			this.queue.putBuffer(buffer);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			this.queue.complete();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
