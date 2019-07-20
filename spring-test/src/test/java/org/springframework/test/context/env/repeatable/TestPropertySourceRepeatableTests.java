package org.springframework.test.context.env.repeatable;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestPropertySource(properties = "first = 1111")
@TestPropertySource(properties = "second = 2222")
public class TestPropertySourceRepeatableTests {

	@Autowired
	Environment env;

	@Value("${first}")
	String first;

	@Value("${second}")
	String second;


	@Test
	public void inlinePropertyFromParentClassAndFromLocalTestPropertySourceAnnotation() {
		assertPropertyValue("first", first, "1111");
		assertPropertyValue("second", second, "2222");
	}

	private void assertPropertyValue(String name, String value, String expectedValue) {
		assertThat(env.getProperty(name)).isEqualTo(expectedValue);
		assertThat(value).isEqualTo(expectedValue);
	}


	@Configuration
	static class Config {
	}
}
