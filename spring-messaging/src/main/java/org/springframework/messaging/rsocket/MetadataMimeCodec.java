/*
 * Copyright 2002-2019 the original author or authors.
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
 *
 * https://github.com/rsocket/rsocket/blob/master/Extensions/PerStreamDataMimeTypesDefinition.md.
 * @author Rudy Steiner
 *
 **/
final public class MetadataMimeCodec {
	static final int STREAM_METADATA_KNOWN_MASK = 0x80; // 1000 0000
	static final byte STREAM_METADATA_LENGTH_MASK = 0x7F; // 0111 1111

	private MetadataMimeCodec() {}
	static ByteBuf  encodeMime(
			ByteBufAllocator allocator, WellKnownMimeType mimeType) {
		return allocator.buffer(1, 1)
				.writeByte(mimeType.getIdentifier() | STREAM_METADATA_KNOWN_MASK);
	}

	//
	public static void encodeMime(
			CompositeByteBuf compositeMetaData,
			ByteBufAllocator allocator,
			String mimeType) {
		WellKnownMimeType wkn = WellKnownMimeType.fromString(mimeType);
		if (wkn == WellKnownMimeType.UNPARSEABLE_MIME_TYPE) {
			compositeMetaData.addComponents(true,encodeMime(allocator,mimeType));
		}
		else {
			compositeMetaData.addComponents(true,encodeMime(allocator,wkn));
		}
	}

	static ByteBuf encodeMime(
			ByteBufAllocator allocator, String customMime) {
		ByteBuf mime = allocator.buffer(1 + customMime.length());
		// reserve 1 byte for the customMime length
		// /!\ careful not to read that first byte, which is random at this point
		int writerIndexInitial = mime.writerIndex();
		mime.writerIndex(writerIndexInitial + 1);

		// write the custom mime in UTF8 but validate it is all ASCII-compatible
		// (which produces the right result since ASCII chars are still encoded on 1 byte in UTF8)
		int customMimeLength = ByteBufUtil.writeUtf8(mime, customMime);
		if (!ByteBufUtil.isText(
				mime, mime.readerIndex() + 1, customMimeLength, CharsetUtil.US_ASCII)) {
			mime.release();
			throw new IllegalArgumentException("custom mime type must be US_ASCII characters only");
		}
		if (customMimeLength < 1 || customMimeLength > 128) {
			mime.release();
			throw new IllegalArgumentException(
					"custom mime type must have a strictly positive length that fits on 7 unsigned bits, ie 1-128");
		}
		mime.markWriterIndex();
		// go back to beginning and write the length
		// encoded length is one less than actual length, since 0 is never a valid length, which gives
		// wider representation range
		mime.writerIndex(writerIndexInitial);
		mime.writeByte(customMimeLength - 1);

		// go back to post-mime type
		mime.resetWriterIndex();
		return mime;
	}

	static List<String> decodeMime(ByteBuf buf){
		List<String> mimes = new ArrayList<String>();
		while(buf.isReadable()){
			byte mimeIdOrLength = buf.readByte();
			if ((mimeIdOrLength & STREAM_METADATA_KNOWN_MASK) == STREAM_METADATA_KNOWN_MASK) {
				byte mimeIdentifier = (byte)( mimeIdOrLength & STREAM_METADATA_LENGTH_MASK);
				mimes.add(WellKnownMimeType.fromIdentifier(mimeIdentifier).toString());
			}
			else{
				int mimeLen = Byte.toUnsignedInt(mimeIdOrLength) + 1;
				mimes.add(buf.readCharSequence(mimeLen, CharsetUtil.US_ASCII).toString());
			}
		}
		return mimes;
	}
}
