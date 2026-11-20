/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestAttributeArgumentResolver}.
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
class RequestAttributeArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void cookieValue() {
		this.service.execute("test");
		assertAttribute("attribute", "test");
	}

	@SuppressWarnings("SameParameterValue")
	private void assertAttribute(String name, @Nullable String expectedValue) {
		assertThat(this.client.getRequestValues().getAttributes().get(name)).isEqualTo(expectedValue);
	}


	private interface Service {

		@GetExchange
		void execute(@RequestAttribute String attribute);

	}

}
