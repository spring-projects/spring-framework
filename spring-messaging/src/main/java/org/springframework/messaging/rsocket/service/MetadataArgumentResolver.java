/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
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
