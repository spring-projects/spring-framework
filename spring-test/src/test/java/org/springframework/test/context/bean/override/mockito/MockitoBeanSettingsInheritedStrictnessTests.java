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

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.quality.Strictness;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;

/**
 * Tests which verify that strictness configured via
 * {@link MockitoBeanSettings @MockitoBeanSettings} is inherited in
 * {@link Nested @Nested} test classes.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(MockitoTestExecutionListener.class)
@MockitoBeanSettings(Strictness.LENIENT)
class MockitoBeanSettingsInheritedStrictnessTests {

	// Should inherit Strictness.LENIENT.
	@Nested
	class NestedTests {

		@Test
		@SuppressWarnings("rawtypes")
		void unnecessaryStub() {
			List list = mock();
			when(list.get(anyInt())).thenReturn("enigma");

			// We intentionally do NOT perform any assertions against the mock,
			// because we want to ensure that an UnnecessaryStubbingException is
			// not thrown by Mockito.
			// assertThat(list.get(1)).isEqualTo("enigma");
		}
	}

}
