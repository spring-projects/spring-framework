/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.framework;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mikaël Francoeur
 * @since 6.2
 * @see CglibProxyExceptionHandlingTests
 */
@DisplayName("JDK proxy exception handling")
class JdkProxyExceptionHandlingTests extends AbstractProxyExceptionHandlingTests {

	@Override
	protected void assertProxyType(Object proxy) {
		assertThat(Proxy.isProxyClass(proxy.getClass())).isTrue();
	}

}
