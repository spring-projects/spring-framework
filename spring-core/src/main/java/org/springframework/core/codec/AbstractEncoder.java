/*
 * Copyright 2002-2019 the original author or authors.
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
 * @param <T> the element type
 */
public abstract class AbstractEncoder<T> implements Encoder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private final List<MimeType> encodableMimeTypes;


	protected AbstractEncoder(MimeType... supportedMimeTypes) {
		this.encodableMimeTypes = Arrays.asList(supportedMimeTypes);
	}


	/**
	 * Set an alternative logger to use than the one based on the class name.
	 * @param logger the logger to use
	 * @since 5.1
	 */
	public void setLogger(Log logger) {
		this.logger = logger;
	}

	/**
	 * Return the currently configured Logger.
	 * @since 5.1
	 */
	public Log getLogger() {
		return logger;
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
		for(MimeType candidate : this.encodableMimeTypes) {
			if (candidate.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}

}
