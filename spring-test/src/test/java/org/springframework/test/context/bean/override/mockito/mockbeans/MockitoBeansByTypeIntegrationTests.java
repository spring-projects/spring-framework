/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.mockbeans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for {@link MockitoBeans @MockitoBeans} and
 * {@link MockitoBean @MockitoBean} declared "by type" at the class level, as a
 * repeatable annotation, and via a custom composed annotation.
 *
 * @author Sam Brannen
 * @since 6.2.2
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/33925">gh-33925</a>
 * @see MockitoBeansByNameIntegrationTests
 */
@SpringJUnitConfig
@MockitoBean(types = {Service04.class, Service05.class})
@SharedMocks // Intentionally declared between local @MockitoBean declarations
@MockitoBean(types = Service06.class)
class MockitoBeansByTypeIntegrationTests implements TestInterface01 {

	@Autowired
	Service01 service01;

	@Autowired
	Service02 service02;

	@Autowired
	Service03 service03;

	@Autowired
	Service04 service04;

	@Autowired
	Service05 service05;

	@Autowired
	Service06 service06;

	@MockitoBean
	Service07 service07;


	@BeforeEach
	void configureMocks() {
		given(service01.greeting()).willReturn("mock 01");
		given(service02.greeting()).willReturn("mock 02");
		given(service03.greeting()).willReturn("mock 03");
		given(service04.greeting()).willReturn("mock 04");
		given(service05.greeting()).willReturn("mock 05");
		given(service06.greeting()).willReturn("mock 06");
		given(service07.greeting()).willReturn("mock 07");
	}

	@Test
	void checkMocks() {
		assertThat(service01.greeting()).isEqualTo("mock 01");
		assertThat(service02.greeting()).isEqualTo("mock 02");
		assertThat(service03.greeting()).isEqualTo("mock 03");
		assertThat(service04.greeting()).isEqualTo("mock 04");
		assertThat(service05.greeting()).isEqualTo("mock 05");
		assertThat(service06.greeting()).isEqualTo("mock 06");
		assertThat(service07.greeting()).isEqualTo("mock 07");
	}


	@MockitoBean(types = Service09.class)
	static class BaseTestCase implements TestInterface08 {

		@Autowired
		Service08 service08;

		@Autowired
		Service09 service09;

		@MockitoBean
		Service10 service10;
	}

	@Nested
	@MockitoBean(types = Service12.class)
	class NestedTests extends BaseTestCase implements TestInterface11 {

		@Autowired
		Service11 service11;

		@Autowired
		Service12 service12;

		@MockitoBean
		Service13 service13;


		@BeforeEach
		void configureMocks() {
			given(service08.greeting()).willReturn("mock 08");
			given(service09.greeting()).willReturn("mock 09");
			given(service10.greeting()).willReturn("mock 10");
			given(service11.greeting()).willReturn("mock 11");
			given(service12.greeting()).willReturn("mock 12");
			given(service13.greeting()).willReturn("mock 13");
		}

		@Test
		void checkMocks() {
			assertThat(service01.greeting()).isEqualTo("mock 01");
			assertThat(service02.greeting()).isEqualTo("mock 02");
			assertThat(service03.greeting()).isEqualTo("mock 03");
			assertThat(service04.greeting()).isEqualTo("mock 04");
			assertThat(service05.greeting()).isEqualTo("mock 05");
			assertThat(service06.greeting()).isEqualTo("mock 06");
			assertThat(service07.greeting()).isEqualTo("mock 07");
			assertThat(service08.greeting()).isEqualTo("mock 08");
			assertThat(service09.greeting()).isEqualTo("mock 09");
			assertThat(service10.greeting()).isEqualTo("mock 10");
			assertThat(service11.greeting()).isEqualTo("mock 11");
			assertThat(service12.greeting()).isEqualTo("mock 12");
			assertThat(service13.greeting()).isEqualTo("mock 13");
		}
	}

}
