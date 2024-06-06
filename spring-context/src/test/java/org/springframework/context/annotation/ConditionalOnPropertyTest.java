package org.springframework.context.annotation;

import org.testng.annotations.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class ConditionalOnPropertyTest {

	@Test
	public void whenPropertyIsSet_thenBeanIsLoaded() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("my.feature.enabled", "true");
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testProperties", properties));
		context.register(MyConfiguration.class);
		context.refresh();

		MyFeatureBean bean = context.getBean(MyFeatureBean.class);
		assertNotNull(bean);
		bean.executeFeature(); // Demonstrate bean functionality
	}

	@Test
	public void whenPropertyIsNotSet_thenBeanIsNotLoaded() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MyConfiguration.class);
		context.refresh();

		MyFeatureBean bean = context.getBean(MyFeatureBean.class);
		assertNull(bean);
	}
}
