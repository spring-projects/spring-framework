/*
 * Copyright 2002-2022 the original author or authors.
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


import org.springframework.test.context.aot.samples.common.DefaultMessageService;
import org.springframework.test.context.aot.samples.common.MessageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
public class BasicVintageTests {

	private final MessageService messageService = new DefaultMessageService();

	@org.junit.Test
	public void test() {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
	}

}
