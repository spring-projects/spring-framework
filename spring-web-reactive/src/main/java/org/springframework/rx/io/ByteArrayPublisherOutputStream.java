package org.springframework.rx.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.reactivestreams.Publisher;

import org.springframework.rx.util.BlockingSignalQueue;

/**
 * {@code OutputStream} implementation that stores all written bytes, to be retrieved
 * using {@link #toByteBufPublisher()}.
 * @author Arjen Poutsma
 */
public class ByteArrayPublisherOutputStream extends OutputStream {

	private final BlockingSignalQueue<byte[]> queue = new BlockingSignalQueue<byte[]>();

	/**
	 * Returns the written data as a {@code Publisher}.
	 * @return a publisher for the written bytes
	 */
	public Publisher<byte[]> toByteBufPublisher() {
		return this.queue.publisher();
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[]{(byte) b});
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		byte[] copy = Arrays.copyOf(b, len);
		try {
			this.queue.putSignal(copy);
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
