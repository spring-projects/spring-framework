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

package org.springframework.http;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
class MediaTypeFactoryTests {

	@Test
	void getMediaType() {
		assertThat(MediaTypeFactory.getMediaType("file.xml")).contains(MediaType.APPLICATION_XML);
		assertThat(MediaTypeFactory.getMediaType("file.js")).contains(MediaType.parseMediaType("text/javascript"));
		assertThat(MediaTypeFactory.getMediaType("file.css")).contains(MediaType.parseMediaType("text/css"));
		assertThat(MediaTypeFactory.getMediaType("file.wasm")).contains(MediaType.parseMediaType("application/wasm"));
		assertThat(MediaTypeFactory.getMediaType("file.foobar")).isNotPresent();
	}

	@Test
	void nullParameter() {
		assertThat(MediaTypeFactory.getMediaType((String) null)).isNotPresent();
		assertThat(MediaTypeFactory.getMediaType((Resource) null)).isNotPresent();
		assertThat(MediaTypeFactory.getMediaTypes(null)).isEmpty();
	}

}
