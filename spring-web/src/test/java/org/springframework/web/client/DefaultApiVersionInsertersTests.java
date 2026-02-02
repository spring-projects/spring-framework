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
 * @author Nabil Fawwaz Elqayyim
 */
class DefaultApiVersionInsertersTests {

	@Test
	void insertVersionViaPathPreservesExistingEncoding() {
		URI result = ApiVersionInserter.usePathSegment(0).insertVersion("1", URI.create("/path?q=%20"));
		assertThat(result.toString()).isEqualTo("/1/path?q=%20");
	}

	@Test
	void insertVersionViaQueryParamPreservesEncoding() {
		URI result = ApiVersionInserter.useQueryParam("version").insertVersion("1", URI.create("/path?q=%20"));
		assertThat(result.toString()).isEqualTo("/path?q=%20&version=1");
	}

}
