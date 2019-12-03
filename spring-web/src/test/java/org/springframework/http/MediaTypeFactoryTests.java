/*
 * Copyright 2002-2019 the original author or authors.
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
 */
public class MediaTypeFactoryTests {

	@Test
	public void getMediaType() {
		assertThat(MediaTypeFactory.getMediaType("file.xml").get()).isEqualTo(MediaType.APPLICATION_XML);
		assertThat(MediaTypeFactory.getMediaType("file.js").get()).isEqualTo(MediaType.parseMediaType("application/javascript"));
		assertThat(MediaTypeFactory.getMediaType("file.css").get()).isEqualTo(MediaType.parseMediaType("text/css"));
		assertThat(MediaTypeFactory.getMediaType("file.foobar").isPresent()).isFalse();
	}

	@Test
	public void nullParameter() {
		assertThat(MediaTypeFactory.getMediaType((String) null).isPresent()).isFalse();
		assertThat(MediaTypeFactory.getMediaType((Resource) null).isPresent()).isFalse();
		assertThat(MediaTypeFactory.getMediaTypes(null).isEmpty()).isTrue();
	}

}
