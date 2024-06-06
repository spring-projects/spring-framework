package org.springframework.context.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfiguration {

	@Bean
	@ConditionalOnProperty(name = "my.feature.enabled", havingValue = "true")
	public MyFeatureBean myFeatureBean() {
		return new MyFeatureBean();
	}
}
