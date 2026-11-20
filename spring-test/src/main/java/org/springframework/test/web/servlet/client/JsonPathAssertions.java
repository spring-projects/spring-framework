/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import com.jayway.jsonpath.Configuration;
import org.jspecify.annotations.Nullable;

import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.support.AbstractJsonPathAssertions;

/**
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 */
public class JsonPathAssertions extends AbstractJsonPathAssertions<RestTestClient.BodyContentSpec> {


	JsonPathAssertions(
			RestTestClient.BodyContentSpec spec, String content, String expression,
			@Nullable Configuration configuration) {

		super(spec, content, expression, configuration);
	}

}
