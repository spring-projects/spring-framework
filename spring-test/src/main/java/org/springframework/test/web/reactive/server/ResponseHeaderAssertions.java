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
package org.springframework.test.web.reactive.server;

import java.util.List;

import org.springframework.util.CollectionUtils;

/**
 * Assertions on the values of a specific header.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseHeaderAssertions extends StringAssertions {

	private final List<String> values;


	ResponseHeaderAssertions(ExchangeActions actions, String headerName, List<String> values) {
		super(actions, initHeaderValue(values), "Response header [" + headerName + "]");
		this.values = values;
	}

	private static String initHeaderValue(List<String> values) {
		return CollectionUtils.isEmpty(values) ? null : values.get(0);
	}


	public ListAssertions<String> values() {
		return new ListAssertions<>(getExchangeActions(), this.values, getErrorPrefix());
	}

}
