/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.net.URI;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link UrlArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings({"DataFlowIssue", "OptionalAssignedToNull"})
class UrlArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void url() {
		URI dynamicUrl = URI.create("dynamic-path");
		this.service.execute(dynamicUrl);

		assertThat(getRequestValues().getUri()).isEqualTo(dynamicUrl);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
	}

	@Test
	void notUrl() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.service.executeNotUri("test"))
				.withMessage("Could not resolve parameter [0] in " +
						"public abstract void org.springframework.web.service.invoker." +
						"UrlArgumentResolverTests$Service.executeNotUri(java.lang.String): " +
						"No suitable resolver");
	}

	@Test
	void nullUrl() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.execute(null))
				.withMessage("URI is required");
	}

	@Test
	void nullUrlWithNullable() {
		this.service.executeNullable(null);

		assertThat(getRequestValues().getUri()).isNull();
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
	}

	@Test
	void nullUrlWithOptional() {
		this.service.executeOptional(null);

		assertThat(getRequestValues().getUri()).isNull();
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
	}

	@Test
	void emptyOptionalUrl() {
		this.service.executeOptional(Optional.empty());

		assertThat(getRequestValues().getUri()).isNull();
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
	}

	@Test
	void optionalUrl() {
		URI dynamicUrl = URI.create("dynamic-path");
		this.service.executeOptional(Optional.of(dynamicUrl));

		assertThat(getRequestValues().getUri()).isEqualTo(dynamicUrl);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
	}


	private HttpRequestValues getRequestValues() {
		return this.client.getRequestValues();
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface Service {

		@GetExchange("/path")
		void execute(URI uri);

		@GetExchange("/path")
		void executeNullable(@Nullable URI uri);

		@GetExchange("/path")
		void executeOptional(Optional<URI> uri);

		@GetExchange
		void executeNotUri(String other);
	}

}
