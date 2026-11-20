/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * with a custom YAML {@link PropertySourceFactory}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@YamlTestProperties("test-properties.yaml")
class YamlTestPropertySourceTests {

	@ParameterizedTest
	@CsvSource(delimiterString = "->", textBlock = """
			environments.dev.url   -> https://dev.example.com
			environments.dev.name  -> 'Developer Setup'
			environments.prod.url  -> https://prod.example.com
			environments.prod.name -> 'My Cool App'
			""")
	void propertyIsAvailableInEnvironment(String property, String value, @Autowired ConfigurableEnvironment env) {
		assertThat(env.getProperty(property)).isEqualTo(value);
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
