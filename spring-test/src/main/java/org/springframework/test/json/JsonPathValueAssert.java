/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import com.jayway.jsonpath.JsonPath;
import org.jspecify.annotations.Nullable;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to a JSON value produced by evaluating a {@linkplain JsonPath JSON path}
 * expression.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class JsonPathValueAssert extends AbstractJsonValueAssert<JsonPathValueAssert> {

	private final String expression;


	JsonPathValueAssert(@Nullable Object actual, String expression, @Nullable JsonConverterDelegate converter) {
		super(actual, JsonPathValueAssert.class, converter);
		this.expression = expression;
	}

	@Override
	protected String getExpectedErrorMessagePrefix() {
		return "Expected value at JSON path \"%s\":".formatted(this.expression);
	}

}
