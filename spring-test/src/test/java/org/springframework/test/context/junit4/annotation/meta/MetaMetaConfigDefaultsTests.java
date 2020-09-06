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

package org.springframework.test.context.junit4.annotation.meta;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-meta-annotation support, relying on default attribute
 * values defined in {@link ConfigClassesAndProfilesWithCustomDefaultsMetaConfig} and
 * overrides in {@link MetaMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@MetaMetaConfig
public class MetaMetaConfigDefaultsTests {

	@Autowired
	private String foo;


	@Test
	public void foo() {
		assertThat(foo).isEqualTo("Production Foo");
	}
}
