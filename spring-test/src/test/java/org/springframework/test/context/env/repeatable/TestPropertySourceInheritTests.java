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
@TestPropertySource(properties = "key = 051187")
public class TestPropertySourceInheritTests extends ParentClassWithTestProperties {

	@Autowired
	Environment env;

	@Value("${key}")
	String key;

	@Value("${inherited}")
	String inherited;


	@Test
	public void inlinePropertyFromParentClassAndFromLocalTestPropertySourceAnnotation() {
		assertThat(env.getProperty("key")).isEqualTo("051187");
		assertThat(this.key).isEqualTo("051187");

		assertThat(env.getProperty("inherited")).isEqualTo("12345");
		assertThat(inherited).isEqualTo("12345");
	}


	@Configuration
	static class Config {
	}
}
