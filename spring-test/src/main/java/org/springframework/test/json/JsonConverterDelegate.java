/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import java.io.IOException;

import org.springframework.core.ResolvableType;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Delegate to abstract JSON type conversion in AssertJ support classes.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface JsonConverterDelegate {

	/**
	 * Convert JSON content to the given {@code targetType}.
	 * @param content the JSON content
	 * @param targetType the target type
	 * @return the decoded object
	 * @param <T> the target type
	 */
	<T> T read(String content, ResolvableType targetType) throws IOException;

	/**
	 * Map the given Object value to the given {@code targetType}, via
	 * serialization and deserialization to and from JSON. This is useful for
	 * mapping generic maps and lists to higher level Objects.
	 * @param value the value to map
	 * @param targetType the target type
	 * @return the decoded object
	 * @param <T> the target type
	 */
	<T> T map(Object value, ResolvableType targetType) throws IOException;


	/**
	 * Create a {@link JsonConverterDelegate} from message converters.
	 * @param candidates the candidates
	 */
	static JsonConverterDelegate of(Iterable<HttpMessageConverter<?>> candidates) {
		return new DefaultJsonConverterDelegate(candidates);
	}

}
