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

package org.springframework.test.context.bean.override.convention;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class AbstractTestBeanIntegrationTestCase {

		@TestBean
		Pojo someBean;

		@TestBean
		Pojo otherBean;

		@TestBean(name = "thirdBean")
		Pojo anotherBean;

		static Pojo otherBeanTestOverride() {
			return new FakePojo("otherBean in superclass");
		}

		static Pojo thirdBeanTestOverride() {
			return new FakePojo("third in superclass");
		}

		static Pojo commonBeanOverride() {
			return new FakePojo("in superclass");
		}

	interface Pojo {

		default String getValue() {
			return "Prod";
		}
	}

	static class ProdPojo implements Pojo { }

	static class FakePojo implements Pojo {
		final String value;

		protected FakePojo(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return getValue();
		}
	}

	@Configuration
	static class Config {

		@Bean
		Pojo someBean() {
			return new ProdPojo();
		}
		@Bean
		Pojo otherBean() {
			return new ProdPojo();
		}
		@Bean
		Pojo thirdBean() {
			return new ProdPojo();
		}
		@Bean
		Pojo pojo() {
			return new ProdPojo();
		}
		@Bean
		Pojo pojo2() {
			return new ProdPojo();
		}
	}
}
