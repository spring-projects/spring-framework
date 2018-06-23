/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.codec;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link Decoder} implementations.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractEncoder<T> implements Encoder<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<MimeType> encodableMimeTypes;


	protected AbstractEncoder(MimeType... supportedMimeTypes) {
		this.encodableMimeTypes = Arrays.asList(supportedMimeTypes);
	}


	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return this.encodableMimeTypes;
	}

	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		return this.encodableMimeTypes.stream().anyMatch(candidate -> candidate.isCompatibleWith(mimeType));
	}

	/**
	 * Helper method to obtain the logger to use from the Map of hints, or fall
	 * back on the default logger. This may be used for example to override
	 * logging, e.g. for a multipart request where the full map of part values
	 * has already been logged.
	 * @param hints the hints passed to the encode method
	 * @return the logger to use
	 * @since 5.1
	 */
	protected Log getLogger(@Nullable Map<String, Object> hints) {
		return hints != null ? ((Log) hints.getOrDefault(Log.class.getName(), logger)) : logger;
	}

}
