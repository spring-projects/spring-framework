/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.multipart.support;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class StandardServletMultipartResolverTests {

	@Test
	public void isMultipartWithDefaultSetting() {
		StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request = new MockHttpServletRequest("PUT", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();
	}

	@Test
	public void isMultipartWithStrictSetting() {
		StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
		resolver.setStrictServletCompliance(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();

		request = new MockHttpServletRequest("PUT", "/");
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		assertThat(resolver.isMultipart(request)).isTrue();

		request.setContentType(MediaType.MULTIPART_MIXED_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();

		request.setContentType(MediaType.MULTIPART_RELATED_VALUE);
		assertThat(resolver.isMultipart(request)).isFalse();
	}

}
