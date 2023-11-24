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

package org.springframework.test.context.env.repeatable;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.env.YamlTestProperties;

/**
 * Analogous to {@link LocalPropertiesFileAndMetaPropertiesFileTests} except
 * that the local file is YAML.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@YamlTestProperties("local.yaml")
@MetaFileTestProperty
class LocalYamlFileAndMetaPropertiesFileTests extends AbstractRepeatableTestPropertySourceTests {

	@Test
	void test() {
		assertEnvironmentValue("key1", "local file");
		assertEnvironmentValue("key2", "meta file");

		assertEnvironmentValue("environments.dev.url", "https://dev.example.com");
		assertEnvironmentValue("environments.dev.name", "Developer Setup");
	}

}
