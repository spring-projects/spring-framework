/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot.samples.basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.aot.samples.common.DefaultMessageService;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.common.SpanishMessageService;
import org.springframework.test.context.aot.samples.management.Managed;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@Configuration(proxyBeanMethods = false)
class BasicTestConfiguration {

	@Bean
	@Profile("default")
	@Managed
	MessageService defaultMessageService() {
		return new DefaultMessageService();
	}

	@Bean
	@Profile("spanish")
	@Managed
	MessageService spanishMessageService() {
		return new SpanishMessageService();
	}

}
