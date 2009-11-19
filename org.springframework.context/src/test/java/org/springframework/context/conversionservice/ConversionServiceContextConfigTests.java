package org.springframework.context.conversionservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConversionServiceContextConfigTests {
	
	@Test
	@Ignore
	public void testConfigOk() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/context/conversionservice/conversionService.xml");
		TestClient client = context.getBean("testClient", TestClient.class);
		assertEquals(2, client.getBars().size());
		assertEquals("value1", client.getBars().get(0).getValue());
		assertEquals("value2", client.getBars().get(1).getValue());
		assertTrue(client.isBool());
	}

}
