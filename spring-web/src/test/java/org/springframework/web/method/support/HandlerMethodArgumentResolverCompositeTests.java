/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test fixture with {@link HandlerMethodArgumentResolverComposite}.
 *
 * @author Rossen Stoyanchev
 */
class HandlerMethodArgumentResolverCompositeTests {

	private HandlerMethodArgumentResolverComposite resolverComposite;

	private MethodParameter paramInt;

	private MethodParameter paramStr;


	@BeforeEach
	void setup() throws Exception {
		this.resolverComposite = new HandlerMethodArgumentResolverComposite();

		Method method = getClass().getDeclaredMethod("handle", Integer.class, String.class);
		paramInt = new MethodParameter(method, 0);
		paramStr = new MethodParameter(method, 1);
	}


	@Test
	void supportsParameter() {
		this.resolverComposite.addResolver(new StubArgumentResolver(Integer.class));

		assertThat(this.resolverComposite.supportsParameter(paramInt)).isTrue();
		assertThat(this.resolverComposite.supportsParameter(paramStr)).isFalse();
	}

	@Test
	void resolveArgument() throws Exception {
		this.resolverComposite.addResolver(new StubArgumentResolver(55));
		Object resolvedValue = this.resolverComposite.resolveArgument(paramInt, null, null, null);

		assertThat(resolvedValue).isEqualTo(55);
	}

	@Test
	void checkArgumentResolverOrder() throws Exception {
		this.resolverComposite.addResolver(new StubArgumentResolver(1));
		this.resolverComposite.addResolver(new StubArgumentResolver(2));
		Object resolvedValue = this.resolverComposite.resolveArgument(paramInt, null, null, null);

		assertThat(resolvedValue).as("Didn't use the first registered resolver").isEqualTo(1);
	}

	@Test
	void noSuitableArgumentResolver() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.resolverComposite.resolveArgument(paramStr, null, null, null));
	}


	@SuppressWarnings("unused")
	private void handle(Integer arg1, String arg2) {
	}

}
