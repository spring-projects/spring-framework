/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Extension of {@link java.io.InputStream} that allows for optimized
 * implementations of message digesting.
 *
 * @author Craig Andrews
 * @since 4.2
 */
abstract class UpdateMessageDigestInputStream extends InputStream {

	/**
	 * Update the message digest with the rest of the bytes in this stream.
	 * <p>Using this method is more optimized since it avoids creating new
	 * byte arrays for each call.
	 * @param messageDigest the message digest to update
	 * @throws IOException when propagated from {@link #read()}
	 */
	public void updateMessageDigest(MessageDigest messageDigest) throws IOException {
		int data;
		while ((data = read()) != -1) {
			messageDigest.update((byte) data);
		}
	}

	/**
	 * Update the message digest with the next len bytes in this stream.
	 * <p>Using this method is more optimized since it avoids creating new
	 * byte arrays for each call.
	 * @param messageDigest the message digest to update
	 * @param len how many bytes to read from this stream and use to update the message digest
	 * @throws IOException when propagated from {@link #read()}
	 */
	public void updateMessageDigest(MessageDigest messageDigest, int len) throws IOException {
		int data;
		int bytesRead = 0;
		while (bytesRead < len && (data = read()) != -1) {
			messageDigest.update((byte) data);
			bytesRead++;
		}
	}

}
