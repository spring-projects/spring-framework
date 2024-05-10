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

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocaleContextThreadLocalAccessor}.
 *
 * @author Tadaya Tsuyukubo
 * @author Rossen Stoyanchev
 */
class LocaleContextThreadLocalAccessorTests {

	private final ContextRegistry registry =
			new ContextRegistry().registerThreadLocalAccessor(new LocaleContextThreadLocalAccessor());


	private static Stream<Arguments> propagation() {
		LocaleContext previousContext = new SimpleLocaleContext(Locale.ENGLISH);
		LocaleContext currentContext = new SimpleLocaleContext(Locale.ENGLISH);
		return Stream.of(Arguments.of(null, currentContext), Arguments.of(previousContext, currentContext));
	}

	@ParameterizedTest
	@MethodSource
	@SuppressWarnings({ "try", "unused" })
	void propagation(LocaleContext previousContext, LocaleContext currentContext) throws Exception {

		ContextSnapshot snapshot = createContextSnapshotFor(currentContext);

		AtomicReference<LocaleContext> contextInScope = new AtomicReference<>();
		AtomicReference<LocaleContext> contextAfterScope = new AtomicReference<>();

		Thread thread = new Thread(() -> {
			LocaleContextHolder.setLocaleContext(previousContext);
			try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
				contextInScope.set(LocaleContextHolder.getLocaleContext());
			}
			contextAfterScope.set(LocaleContextHolder.getLocaleContext());
		});

		thread.start();
		thread.join(1000);

		assertThat(contextAfterScope).hasValueSatisfying(value -> assertThat(value).isSameAs(previousContext));
		assertThat(contextInScope).hasValueSatisfying(value -> assertThat(value).isSameAs(currentContext));
	}

	private ContextSnapshot createContextSnapshotFor(LocaleContext context) {
		LocaleContextHolder.setLocaleContext(context);
		try {
			return ContextSnapshotFactory.builder()
					.contextRegistry(this.registry).clearMissing(true).build()
					.captureAll();
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

}
