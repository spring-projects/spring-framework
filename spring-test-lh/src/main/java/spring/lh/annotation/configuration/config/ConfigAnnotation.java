package spring.lh.annotation.configuration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.lh.annotation.configuration.bean.Person;

@Configuration
public class ConfigAnnotation {
	@Bean
	public Person person() {
		return new Person();
	}
}
