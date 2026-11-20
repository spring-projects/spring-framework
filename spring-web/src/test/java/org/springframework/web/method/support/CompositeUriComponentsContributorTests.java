/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeUriComponentsContributor}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class CompositeUriComponentsContributorTests {

	@Test
	void supportsParameter() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		resolvers.add(new RequestParamMethodArgumentResolver(false));
		resolvers.add(new RequestHeaderMethodArgumentResolver(null));
		resolvers.add(new RequestParamMethodArgumentResolver(true));

		CompositeUriComponentsContributor contributor = new CompositeUriComponentsContributor(resolvers);
		Method method = ClassUtils.getMethod(getClass(), "handleRequest", String.class, String.class, String.class);
		assertThat(contributor.supportsParameter(new MethodParameter(method, 0))).isTrue();
		assertThat(contributor.supportsParameter(new MethodParameter(method, 1))).isTrue();
		assertThat(contributor.supportsParameter(new MethodParameter(method, 2))).isFalse();
	}

	@Test
	void hasContributors() {
		assertThat(new CompositeUriComponentsContributor().hasContributors()).isFalse();
		assertThat(new CompositeUriComponentsContributor(new RequestParamMethodArgumentResolver(true)).hasContributors()).isTrue();
	}


	public void handleRequest(@RequestParam String p1, String p2, @RequestHeader String h) {
	}

}
