/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.rsocket.service;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link RSocketServiceArgumentResolver} for metadata entries.
 *
 * <p>Supports a sequence of an {@link Object} parameter for the metadata value,
 * followed by a {@link MimeType} parameter for the metadata mime type.
 *
 * <p>This should be ordered last to give other, more specific resolvers a
 * chance to resolve the argument.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class MetadataArgumentResolver implements RSocketServiceArgumentResolver {

	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, RSocketRequestValues.Builder requestValues) {

		int index = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();

		if (parameter.getParameterType().equals(MimeType.class)) {
			Assert.notNull(argument, "MimeType parameter is required");
			Assert.state(index > 0, "MimeType parameter should have preceding metadata object parameter");
			requestValues.addMimeType((MimeType) argument);
			return true;
		}

		if (paramTypes.length > (index + 1) && MimeType.class.equals(paramTypes[index + 1])) {
			Assert.notNull(argument, "MimeType parameter is required");
			requestValues.addMetadata(argument);
			return true;
		}

		return false;
	}

}
