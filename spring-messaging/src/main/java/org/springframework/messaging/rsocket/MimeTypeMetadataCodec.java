/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.messaging.rsocket;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.CharsetUtil;
import io.rsocket.metadata.WellKnownMimeType;



/**
 * Provides support for encoding and decoding the per-stream MIME type to use for payload data.
 *
 * <p>For more on the format of the metadata, see the <a
 * href="https://github.com/rsocket/rsocket/blob/master/Extensions/PerStreamDataMimeTypesDefinition.md">
 * Stream Data MIME Types</a> extension specification.
 * @author Rudy Steiner
 * @since 1.1.1
 */
public final class MimeTypeMetadataCodec {
	private static final int STREAM_METADATA_KNOWN_MASK = 0x80; // 1000 0000

	private static final byte STREAM_METADATA_LENGTH_MASK = 0x7F; // 0111 1111

	private MimeTypeMetadataCodec() {}

	/**
	* Encode a {@link WellKnownMimeType} into a newly allocated single byte {@link ByteBuf}.
	*
	* @param allocator the allocator to create the buffer with
	* @param mimeType well-known MIME type to encode
	* @return the resulting buffer
	*/
	public static ByteBuf encode(ByteBufAllocator allocator, WellKnownMimeType mimeType) {
		return allocator.buffer(1, 1).writeByte(mimeType.getIdentifier() | STREAM_METADATA_KNOWN_MASK);
	}

	/**
	* Encode the given MIME type into a newly allocated {@link ByteBuf}.
	*
	* @param allocator the allocator to create the buffer with
	* @param mimeType mime type to encode
	* @return the resulting buffer
	*/
	public static ByteBuf encode(ByteBufAllocator allocator, String mimeType) {
		if (mimeType == null || mimeType.length() == 0) {
			throw new IllegalArgumentException("MIME type is required");
		}
		WellKnownMimeType wkn = WellKnownMimeType.fromString(mimeType);
		if (wkn == WellKnownMimeType.UNPARSEABLE_MIME_TYPE) {
			return encodeCustomMimeType(allocator, mimeType);
		}
		else {
			return encode(allocator, wkn);
		}
	}

	/**
	* Encode multiple MIME types into a newly allocated {@link ByteBuf}.
	*
	* @param allocator the allocator to create the buffer with
	* @param mimeTypes mime types to encode
	* @return the resulting buffer
	*/
	public static ByteBuf encode(ByteBufAllocator allocator, List<String> mimeTypes) {
		if (mimeTypes == null || mimeTypes.size() == 0) {
			throw new IllegalArgumentException("No MIME types provided");
		}
		CompositeByteBuf compositeByteBuf = allocator.compositeBuffer();
		for (String mimeType : mimeTypes) {
			ByteBuf byteBuf = encode(allocator, mimeType);
			compositeByteBuf.addComponents(true, byteBuf);
		}
		return compositeByteBuf;
	}

	private static ByteBuf encodeCustomMimeType(ByteBufAllocator allocator, String customMimeType) {
		ByteBuf byteBuf = allocator.buffer(1 + customMimeType.length());
		byteBuf.writerIndex(1);
		int length = ByteBufUtil.writeUtf8(byteBuf, customMimeType);

		if (!ByteBufUtil.isText(byteBuf, 1, length, CharsetUtil.US_ASCII)) {
			byteBuf.release();
			throw new IllegalArgumentException("MIME type must be ASCII characters only");
		}

		if (length < 1 || length > 128) {
			byteBuf.release();
			throw new IllegalArgumentException(
				"MIME type must have a strictly positive length that fits on 7 unsigned bits, ie 1-128");
		}

		byteBuf.markWriterIndex();
		byteBuf.writerIndex(0);
		byteBuf.writeByte(length - 1);
		byteBuf.resetWriterIndex();

		return byteBuf;
	}

	/**
	* Decode the per-stream MIME type metadata encoded in the given {@link ByteBuf}.
	*
	* @return the decoded MIME types
	*/
	public static List<String> decode(ByteBuf byteBuf) {
		List<String> mimeTypes = new ArrayList<>();
		while (byteBuf.isReadable()) {
			byte idOrLength = byteBuf.readByte();
			if ((idOrLength & STREAM_METADATA_KNOWN_MASK) == STREAM_METADATA_KNOWN_MASK) {
				byte id = (byte) (idOrLength & STREAM_METADATA_LENGTH_MASK);
				WellKnownMimeType wellKnownMimeType = WellKnownMimeType.fromIdentifier(id);
				mimeTypes.add(wellKnownMimeType.toString());
			}
			else {
				int length = Byte.toUnsignedInt(idOrLength) + 1;
				mimeTypes.add(byteBuf.readCharSequence(length, CharsetUtil.US_ASCII).toString());
			}
		}
		return mimeTypes;
	}
}
