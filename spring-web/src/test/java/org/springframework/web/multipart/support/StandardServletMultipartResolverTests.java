/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart.support;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class StandardServletMultipartResolverTests {

	@Test
	void isMultipartWithDefaultSetting() {
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
	void isMultipartWithStrictSetting() {
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
