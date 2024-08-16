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

package org.springframework.test.context.bean.override.mockito;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.quality.Strictness;

import org.springframework.test.context.bean.override.mockito.MockitoBeanForByNameLookupIntegrationTests.Config;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Integration tests ensuring unnecessary stubbings are reported in various
 * cases where a strict style is chosen or assumed.
 *
 * @author Simon Baslé
 * @since 6.2
 */
class MockitoBeanSettingsStrictIntegrationTests {

	@ParameterizedTest
	@FieldSource("strictCases")
	void unusedStubbingIsReported(Class<?> forCase) {
		Events events = EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(forCase))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(1).failed(1));

		events.assertThatEvents().haveExactly(1,
				event(test("unnecessaryStub"),
						finishedWithFailure(
								instanceOf(UnnecessaryStubbingException.class),
								message(msg -> msg.contains("Unnecessary stubbings detected.")))));
	}

	static final List<Arguments> strictCases = List.of(
			argumentSet("explicit strictness", ExplicitStrictness.class),
			argumentSet("implicit strictness with @MockitoBean on field", ImplicitStrictnessWithMockitoBean.class)
		);

	abstract static class BaseCase {

		@Test
		@SuppressWarnings("rawtypes")
		void unnecessaryStub() {
			List list = Mockito.mock(List.class);
			Mockito.when(list.get(Mockito.anyInt())).thenReturn(new Object());
		}
	}

	@SpringJUnitConfig(Config.class)
	@MockitoBeanSettings(Strictness.STRICT_STUBS)
	static class ExplicitStrictness extends BaseCase {
	}

	@SpringJUnitConfig(Config.class)
	static class ImplicitStrictnessWithMockitoBean extends BaseCase {

		@MockitoBean
		@SuppressWarnings("unused")
		DateTimeFormatter ignoredMock;
	}

}
