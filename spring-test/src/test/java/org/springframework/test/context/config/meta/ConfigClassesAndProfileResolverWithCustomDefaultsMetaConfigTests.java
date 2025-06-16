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

package org.springframework.test.context.config.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, relying on
 * default attribute values defined in {@link ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ExtendWith(SpringExtension.class)
@ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig
class ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfigTests {

	@Autowired
	private String foo;


	@Test
	void foo() {
		assertThat(foo).isEqualTo("Resolver Foo");
	}

}
