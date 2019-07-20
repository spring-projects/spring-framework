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
@AnnotationWithTestProperty
public class TestPropertySourceInheritedFromMetaAnnotationTests {

	@Autowired
	Environment env;

	@Value("${key}")
	String key;

	@Value("${meta}")
	String meta;


	@Test
	public void inlineLocalPropertyAndPropertyFromMetaAnnotation() {
		// local inlined:
		assertThat(env.getProperty("key")).isEqualTo("051187");
		assertThat(this.key).isEqualTo("051187");
		// inlined from meta-annotation:
		assertThat(env.getProperty("meta")).isEqualTo("value from meta-annotation");
		assertThat(meta).isEqualTo("value from meta-annotation");
	}


	@Configuration
	static class Config {
	}
}
