/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
