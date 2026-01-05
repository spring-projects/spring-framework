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

package org.springframework.web.client;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultApiVersionInserter}.
 *
 * @author Nabil Fawwaz Elqayyim
 */
class DefaultApiVersionInsertersTests {

	@Test
	void insertVersionPreservesExistingEncoding() {
		URI uri = URI.create("http://localhost/test?foo=%20");
		DefaultApiVersionInserter inserter = (DefaultApiVersionInserter) ApiVersionInserter.usePathSegment(0);

		URI result = inserter.insertVersion("1", uri);

		assertThat(result.toString()).isEqualTo("http://localhost/1/test?foo=%20");
	}

	@Test
	void insertVersionAsQueryParamPreservesEncoding() {
		URI uri = URI.create("http://localhost/test?foo=%20");
		DefaultApiVersionInserter inserter = (DefaultApiVersionInserter) ApiVersionInserter.useQueryParam("v");

		URI result = inserter.insertVersion("1", uri);

		assertThat(result.toString()).isEqualTo("http://localhost/test?foo=%20&v=1");
	}

}
