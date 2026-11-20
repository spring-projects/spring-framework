/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.conversionservice;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
class ConversionServiceContextConfigTests {

	@Test
	void configOk() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/context/conversionservice/conversionService.xml")) {
			TestClient client = context.getBean("testClient", TestClient.class);
			assertThat(client.getBars()).hasSize(2);
			assertThat(client.getBars().get(0).getValue()).isEqualTo("value1");
			assertThat(client.getBars().get(1).getValue()).isEqualTo("value2");
			assertThat(client.isBool()).isTrue();
		}
	}

}
