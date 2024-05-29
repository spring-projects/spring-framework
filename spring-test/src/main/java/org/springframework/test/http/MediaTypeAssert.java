/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.http;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied
 * to a {@link MediaType}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.2
 */
public class MediaTypeAssert extends AbstractObjectAssert<MediaTypeAssert, MediaType> {

	public MediaTypeAssert(@Nullable String actual) {
		this(StringUtils.hasText(actual) ? MediaType.parseMediaType(actual) : null);
	}

	public MediaTypeAssert(@Nullable MediaType mediaType) {
		super(mediaType, MediaTypeAssert.class);
		as("Media type");
	}


	/**
	 * Verify that the actual media type is equal to the given string
	 * representation.
	 * @param mediaType the expected media type, as a String to be parsed
	 * into a MediaType
*/
	public MediaTypeAssert isEqualTo(String mediaType) {
		return isEqualTo(parseMediaType(mediaType));
	}

	/**
	 * Verify that the actual media type is not equal to the given string
	 * representation.
	 * @param mediaType the given media type, as a String to be parsed
	 * into a MediaType
	 */
	public MediaTypeAssert isNotEqualTo(String mediaType) {
		return isNotEqualTo(parseMediaType(mediaType));
	}

	/**
	 * Verify that the actual media type is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with the
	 * given one.
	 * <p>Example: <pre><code class='java'>
	 * // Check that actual is compatible with "application/json"
	 * assertThat(mediaType).isCompatibleWith(MediaType.APPLICATION_JSON);
	 * </code></pre>
	 * @param mediaType the media type with which to compare
	 */
	public MediaTypeAssert isCompatibleWith(MediaType mediaType) {
		Assertions.assertThat(this.actual)
				.withFailMessage("Expecting null to be compatible with '%s'", mediaType).isNotNull();
		Assertions.assertThat(mediaType)
				.withFailMessage("Expecting '%s' to be compatible with null", this.actual).isNotNull();
		Assertions.assertThat(this.actual.isCompatibleWith(mediaType))
				.as("check media type '%s' is compatible with '%s'", this.actual.toString(), mediaType.toString())
				.isTrue();
		return this;
	}

	/**
	 * Verify that the actual media type is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with the
	 * given one.
	 * <p>Example: <pre><code class='java'>
	 * // Check that actual is compatible with "text/plain"
	 * assertThat(mediaType).isCompatibleWith("text/plain");
	 * </code></pre>
	 * @param mediaType the media type with which to compare, as a String
	 * to be parsed into a MediaType
	 */
	public MediaTypeAssert isCompatibleWith(String mediaType) {
		return isCompatibleWith(parseMediaType(mediaType));
	}


	@SuppressWarnings("NullAway")
	private MediaType parseMediaType(String value) {
		try {
			return MediaType.parseMediaType(value);
		}
		catch (InvalidMediaTypeException ex) {
			throw Failures.instance().failure(this.info, new ShouldBeValidMediaType(value, ex.getMessage()));
		}
	}

	private static final class ShouldBeValidMediaType extends BasicErrorMessageFactory {

		private ShouldBeValidMediaType(String mediaType, String errorMessage) {
			super("%nExpecting:%n  %s%nTo be a valid media type but got:%n  %s%n", mediaType, errorMessage);
		}
	}

}
