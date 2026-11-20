/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
