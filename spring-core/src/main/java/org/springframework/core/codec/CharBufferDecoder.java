/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decode from a data buffer stream to a {@code CharBuffer} stream, either splitting
 * or aggregating incoming data chunks to realign along newlines delimiters
 * and produce a stream of char buffers. This is useful for streaming but is also
 * necessary to ensure that multi-byte characters can be decoded correctly,
 * avoiding split-character issues. The default delimiters used by default are
 * {@code \n} and {@code \r\n} but that can be customized.
 *
 * @author Markus Heiden
 * @author Arjen Poutsma
 * @since 6.1
 * @see CharSequenceEncoder
 */
public final class CharBufferDecoder extends AbstractCharSequenceDecoder<CharBuffer> {

	public CharBufferDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
		super(delimiters, stripDelimiter, mimeTypes);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (elementType.resolve() == CharBuffer.class) && super.canDecode(elementType, mimeType);
	}

	@Override
	protected CharBuffer decodeInternal(DataBuffer dataBuffer, Charset charset) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
		dataBuffer.toByteBuffer(byteBuffer);
		return charset.decode(byteBuffer);
	}


	/**
	 * Create a {@code CharBufferDecoder} for {@code "text/plain"}.
	 */
	public static CharBufferDecoder textPlainOnly() {
		return textPlainOnly(DEFAULT_DELIMITERS, true);
	}

	/**
	 * Create a {@code CharBufferDecoder} for {@code "text/plain"}.
	 * @param delimiters delimiter strings to use to split the input stream
	 * @param stripDelimiter whether to remove delimiters from the resulting input strings
	 */
	public static CharBufferDecoder textPlainOnly(List<String> delimiters, boolean stripDelimiter) {
		MimeType mimeType = new MimeType("text", "plain", DEFAULT_CHARSET);
		return new CharBufferDecoder(delimiters, stripDelimiter, mimeType);
	}

	/**
	 * Create a {@code CharBufferDecoder} that supports all MIME types.
	 */
	public static CharBufferDecoder allMimeTypes() {
		return allMimeTypes(DEFAULT_DELIMITERS, true);
	}

	/**
	 * Create a {@code CharBufferDecoder} that supports all MIME types.
	 * @param delimiters delimiter strings to use to split the input stream
	 * @param stripDelimiter whether to remove delimiters from the resulting input strings
	 */
	public static CharBufferDecoder allMimeTypes(List<String> delimiters, boolean stripDelimiter) {
		MimeType mimeType = new MimeType("text", "plain", DEFAULT_CHARSET);
		return new CharBufferDecoder(delimiters, stripDelimiter, mimeType, MimeTypeUtils.ALL);
	}

}
