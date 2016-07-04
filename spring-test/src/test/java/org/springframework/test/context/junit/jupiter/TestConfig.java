/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit.jupiter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.junit.jupiter.comics.Cat;
import org.springframework.test.context.junit.jupiter.comics.Dog;
import org.springframework.test.context.junit.jupiter.comics.Person;

/**
 * Demo config for tests.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@Configuration
public class TestConfig {

	@Bean
	static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	Person dilbert() {
		return new Person("Dilbert");
	}

	@Bean
	Person wally() {
		return new Person("Wally");
	}

	@Bean
	Dog dogbert() {
		return new Dog("Dogbert");
	}

	@Primary
	@Bean
	Cat catbert() {
		return new Cat("Catbert");
	}

	@Bean
	Cat garfield() {
		return new Cat("Garfield");
	}

}
