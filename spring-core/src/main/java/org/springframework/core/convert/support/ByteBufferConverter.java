/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

/**
 * Converts a {@link ByteBuffer} directly to and from {@code byte[]}s and indirectly
 * to any type that the {@link ConversionService} support via {@code byte[]}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
final class ByteBufferConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor BYTE_BUFFER_TYPE = TypeDescriptor.valueOf(ByteBuffer.class);

	private static final TypeDescriptor BYTE_ARRAY_TYPE = TypeDescriptor.valueOf(byte[].class);

	private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS;

	static {
		Set<ConvertiblePair> convertiblePairs = new HashSet<>(4);
		convertiblePairs.add(new ConvertiblePair(ByteBuffer.class, byte[].class));
		convertiblePairs.add(new ConvertiblePair(byte[].class, ByteBuffer.class));
		convertiblePairs.add(new ConvertiblePair(ByteBuffer.class, Object.class));
		convertiblePairs.add(new ConvertiblePair(Object.class, ByteBuffer.class));
		CONVERTIBLE_PAIRS = Collections.unmodifiableSet(convertiblePairs);
	}


	private final ConversionService conversionService;


	public ByteBufferConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_PAIRS;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		boolean byteBufferTarget = targetType.isAssignableTo(BYTE_BUFFER_TYPE);
		if (sourceType.isAssignableTo(BYTE_BUFFER_TYPE)) {
			return (byteBufferTarget || matchesFromByteBuffer(targetType));
		}
		return (byteBufferTarget && matchesToByteBuffer(sourceType));
	}

	private boolean matchesFromByteBuffer(TypeDescriptor targetType) {
		return (targetType.isAssignableTo(BYTE_ARRAY_TYPE) ||
				this.conversionService.canConvert(BYTE_ARRAY_TYPE, targetType));
	}

	private boolean matchesToByteBuffer(TypeDescriptor sourceType) {
		return (sourceType.isAssignableTo(BYTE_ARRAY_TYPE) ||
				this.conversionService.canConvert(sourceType, BYTE_ARRAY_TYPE));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		boolean byteBufferTarget = targetType.isAssignableTo(BYTE_BUFFER_TYPE);
		if (source instanceof ByteBuffer) {
			ByteBuffer buffer = (ByteBuffer) source;
			return (byteBufferTarget ? buffer.duplicate() : convertFromByteBuffer(buffer, targetType));
		}
		if (byteBufferTarget) {
			return convertToByteBuffer(source, sourceType);
		}
		// Should not happen
		throw new IllegalStateException("Unexpected source/target types");
	}

	@Nullable
	private Object convertFromByteBuffer(ByteBuffer source, TypeDescriptor targetType) {
		byte[] bytes = new byte[source.remaining()];
		source.get(bytes);

		if (targetType.isAssignableTo(BYTE_ARRAY_TYPE)) {
			return bytes;
		}
		return this.conversionService.convert(bytes, BYTE_ARRAY_TYPE, targetType);
	}

	private Object convertToByteBuffer(@Nullable Object source, TypeDescriptor sourceType) {
		byte[] bytes = (byte[]) (source instanceof byte[] ? source :
				this.conversionService.convert(source, sourceType, BYTE_ARRAY_TYPE));

		if (bytes == null) {
			return ByteBuffer.wrap(new byte[0]);
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
		byteBuffer.put(bytes);

		// Extra cast necessary for compiling on JDK 9 plus running on JDK 8, since
		// otherwise the overridden ByteBuffer-returning rewind method would be chosen
		// which isn't available on JDK 8.
		return ((Buffer) byteBuffer).rewind();
	}

}
