/*
 * Copyright 2002-present the original author or authors.
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
