/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link org.springframework.web.accept.PathApiVersionResolver}.
 * @author Rossen Stoyanchev
 * @author Martin Mois
 */
class PathApiVersionResolverTests {

	@Test
	void resolve() {
		testResolve(0, "/1.0/path", "1.0");
		testResolve(1, "/app/1.1/path", "1.1");
	}

	@Test
	void insufficientPathSegments() {
		assertThatThrownBy(() -> testResolve(0, "/", "1.0")).isInstanceOf(InvalidApiVersionException.class);
	}

	@Test
	void resolveWithVersionPathPredicate() {
		testVersionPathPredicate("/app/1.0/path", "1.0");
		testVersionPathPredicate("/app", null);
		testVersionPathPredicate("/v3/api-docs", null);
	}

	private static void testVersionPathPredicate(String requestUri, String expected) {
		Predicate<RequestPath> versionPathPredicate = path -> {
			List<PathContainer.Element> elements = path.elements();
			return (elements.size() > 3 &&
					elements.get(1).value().equals("app") &&
					elements.get(3).value().matches("\\d+\\.\\d+"));
		};
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(requestUri));
		PathApiVersionResolver resolver = new PathApiVersionResolver(1, versionPathPredicate);
		String actual = resolver.resolveVersion(exchange);
		assertThat(actual).isEqualTo(expected);
	}

	private static void testResolve(int index, String requestUri, String expected) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(requestUri));
		String actual = new PathApiVersionResolver(index).resolveVersion(exchange);
		assertThat(actual).isEqualTo(expected);
	}
}
