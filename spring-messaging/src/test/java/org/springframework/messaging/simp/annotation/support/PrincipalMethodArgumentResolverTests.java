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

package org.springframework.messaging.simp.annotation.support;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrincipalMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Johnny Lim
 */
class PrincipalMethodArgumentResolverTests {

	private final PrincipalMethodArgumentResolver resolver = new PrincipalMethodArgumentResolver();

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Principal.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Optional.class, Principal.class))).isTrue();
	}


	@Test
	void resolverArgument() {
		Principal user = () -> "Joe";
		Message<String> message = new GenericMessage<>("Hello, world!",
				Collections.singletonMap(SimpMessageHeaderAccessor.USER_HEADER, user));

		MethodParameter param = this.testMethod.arg(Principal.class);
		Object actual = this.resolver.resolveArgument(param, message);
		assertThat(actual).isSameAs(user);

		param = this.testMethod.arg(Optional.class, Principal.class);
		actual = this.resolver.resolveArgument(param, message);
		assertThat(actual).isInstanceOf(Optional.class).extracting(o -> ((Optional<?>) o).get()).isSameAs(user);
	}


	@SuppressWarnings("unused")
	void handle(Principal user, Optional<Principal> optionalUser) {
	}

}
