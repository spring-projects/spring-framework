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

package org.springframework.test.context.bean.override.mockito.typelevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBeans;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;

/**
 * Integration tests for {@link MockitoSpyBeans @MockitoSpyBeans} and
 * {@link MockitoSpyBean @MockitoSpyBean} declared "by type" at the class level,
 * as a repeatable annotation, and via a custom composed annotation.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34408">gh-34408</a>
 * @see MockitoSpyBeansByNameIntegrationTests
 */
@SpringJUnitConfig
@MockitoSpyBean(types = {Service04.class, Service05.class})
@SharedSpies // Intentionally declared between local @MockitoSpyBean declarations
@MockitoSpyBean(types = Service06.class)
class MockitoSpyBeansByTypeIntegrationTests implements SpyTestInterface01 {

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

	@MockitoSpyBean
	Service07 service07;


	@BeforeEach
	void configureSpies() {
		given(service01.greeting()).willReturn("spy 01");
		given(service02.greeting()).willReturn("spy 02");
		given(service03.greeting()).willReturn("spy 03");
		given(service04.greeting()).willReturn("spy 04");
		given(service05.greeting()).willReturn("spy 05");
		given(service06.greeting()).willReturn("spy 06");
		given(service07.greeting()).willReturn("spy 07");
	}

	@Test
	void checkSpies() {
		assertIsSpy(service01, "service01");
		assertIsSpy(service02, "service02");
		assertIsSpy(service03, "service03");
		assertIsSpy(service04, "service04");
		assertIsSpy(service05, "service05");
		assertIsSpy(service06, "service06");
		assertIsSpy(service07, "service07");

		assertThat(service01.greeting()).isEqualTo("spy 01");
		assertThat(service02.greeting()).isEqualTo("spy 02");
		assertThat(service03.greeting()).isEqualTo("spy 03");
		assertThat(service04.greeting()).isEqualTo("spy 04");
		assertThat(service05.greeting()).isEqualTo("spy 05");
		assertThat(service06.greeting()).isEqualTo("spy 06");
		assertThat(service07.greeting()).isEqualTo("spy 07");
	}


	@MockitoSpyBean(types = Service09.class)
	class BaseTestCase implements SpyTestInterface08 {

		@Autowired
		Service08 service08;

		@Autowired
		Service09 service09;

		@MockitoSpyBean
		Service10 service10;
	}

	@Nested
	@MockitoSpyBean(types = Service12.class)
	class NestedTests extends BaseTestCase implements SpyTestInterface11 {

		@Autowired
		Service11 service11;

		@Autowired
		Service12 service12;

		@MockitoSpyBean
		Service13 service13;


		@BeforeEach
		void configureSpies() {
			given(service08.greeting()).willReturn("spy 08");
			given(service09.greeting()).willReturn("spy 09");
			given(service10.greeting()).willReturn("spy 10");
			given(service11.greeting()).willReturn("spy 11");
			given(service12.greeting()).willReturn("spy 12");
			given(service13.greeting()).willReturn("spy 13");
		}

		@Test
		void checkSpies() {
			assertIsSpy(service01, "service01");
			assertIsSpy(service02, "service02");
			assertIsSpy(service03, "service03");
			assertIsSpy(service04, "service04");
			assertIsSpy(service05, "service05");
			assertIsSpy(service06, "service06");
			assertIsSpy(service07, "service07");
			assertIsSpy(service08, "service08");
			assertIsSpy(service09, "service09");
			assertIsSpy(service10, "service10");
			assertIsSpy(service11, "service11");
			assertIsSpy(service12, "service12");
			assertIsSpy(service13, "service13");

			assertThat(service01.greeting()).isEqualTo("spy 01");
			assertThat(service02.greeting()).isEqualTo("spy 02");
			assertThat(service03.greeting()).isEqualTo("spy 03");
			assertThat(service04.greeting()).isEqualTo("spy 04");
			assertThat(service05.greeting()).isEqualTo("spy 05");
			assertThat(service06.greeting()).isEqualTo("spy 06");
			assertThat(service07.greeting()).isEqualTo("spy 07");
			assertThat(service08.greeting()).isEqualTo("spy 08");
			assertThat(service09.greeting()).isEqualTo("spy 09");
			assertThat(service10.greeting()).isEqualTo("spy 10");
			assertThat(service11.greeting()).isEqualTo("spy 11");
			assertThat(service12.greeting()).isEqualTo("spy 12");
			assertThat(service13.greeting()).isEqualTo("spy 13");
		}
	}


	@Configuration
	static class Config {

		@Bean
		Service01 service01() {
			return new Service01() {
				@Override
				public String greeting() {
					return "prod 1";
				}
			};
		}

		@Bean
		Service02 service02() {
			return new Service02() {
				@Override
				public String greeting() {
					return "prod 2";
				}
			};
		}

		@Bean
		Service03 service03() {
			return new Service03() {
				@Override
				public String greeting() {
					return "prod 3";
				}
			};
		}

		@Bean
		Service04 service04() {
			return new Service04() {
				@Override
				public String greeting() {
					return "prod 4";
				}
			};
		}

		@Bean
		Service05 service05() {
			return new Service05() {
				@Override
				public String greeting() {
					return "prod 5";
				}
			};
		}

		@Bean
		Service06 service06() {
			return new Service06() {
				@Override
				public String greeting() {
					return "prod 6";
				}
			};
		}

		@Bean
		Service07 service07() {
			return new Service07() {
				@Override
				public String greeting() {
					return "prod 7";
				}
			};
		}

		@Bean
		Service08 service08() {
			return new Service08() {
				@Override
				public String greeting() {
					return "prod 8";
				}
			};
		}

		@Bean
		Service09 service09() {
			return new Service09() {
				@Override
				public String greeting() {
					return "prod 9";
				}
			};
		}

		@Bean
		Service10 service10() {
			return new Service10() {
				@Override
				public String greeting() {
					return "prod 10";
				}
			};
		}

		@Bean
		Service11 service11() {
			return new Service11() {
				@Override
				public String greeting() {
					return "prod 11";
				}
			};
		}

		@Bean
		Service12 service12() {
			return new Service12() {
				@Override
				public String greeting() {
					return "prod 12";
				}
			};
		}

		@Bean
		Service13 service13() {
			return new Service13() {
				@Override
				public String greeting() {
					return "prod 13";
				}
			};
		}
	}

}
