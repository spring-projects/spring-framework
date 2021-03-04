/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.tags;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Param}.
 *
 * @author Scott Andrews
 */
public class ParamTests {

	private final Param param = new Param();

	@Test
	public void name() {
		param.setName("name");
		assertThat(param.getName()).isEqualTo("name");
	}

	@Test
	public void value() {
		param.setValue("value");
		assertThat(param.getValue()).isEqualTo("value");
	}

	@Test
	public void nullDefaults() {
		assertThat(param.getName()).isNull();
		assertThat(param.getValue()).isNull();
	}

}
