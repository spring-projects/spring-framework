/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.method.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.web.method.support.CompositeUriComponentsContributor}.
 *
 * @author Rossen Stoyanchev
 */
public class CompositeUriComponentsContributorTests {


	@Test
	public void supportsParameter() {

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();
		resolvers.add(new RequestParamMethodArgumentResolver(false));
		resolvers.add(new RequestHeaderMethodArgumentResolver(null));
		resolvers.add(new RequestParamMethodArgumentResolver(true));

		Method method = ClassUtils.getMethod(this.getClass(), "handleRequest", String.class, String.class, String.class);

		CompositeUriComponentsContributor contributor = new CompositeUriComponentsContributor(resolvers);
		assertTrue(contributor.supportsParameter(new MethodParameter(method, 0)));
		assertTrue(contributor.supportsParameter(new MethodParameter(method, 1)));
		assertFalse(contributor.supportsParameter(new MethodParameter(method, 2)));
	}


	public void handleRequest(@RequestParam String p1, String p2, @RequestHeader String h) {
	}

}
