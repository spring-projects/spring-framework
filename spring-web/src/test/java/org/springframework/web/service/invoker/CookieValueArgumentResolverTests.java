/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CookieValueArgumentResolver}.
 *
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
class CookieValueArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void cookieValue() {
		this.service.execute("test");
		assertCookie("cookie", "test");
	}

	private void assertCookie(String key, String... values) {
		List<String> actualValues = this.client.getRequestValues().getCookies().get(key);
		if (ObjectUtils.isEmpty(values)) {
			assertThat(actualValues).isNull();
		}
		else {
			assertThat(actualValues).containsOnly(values);
		}
	}


	private interface Service {

		@GetExchange
		void execute(@CookieValue String cookie);

	}

}
