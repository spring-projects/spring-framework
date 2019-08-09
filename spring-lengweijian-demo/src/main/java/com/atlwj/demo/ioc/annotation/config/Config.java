package com.atlwj.demo.ioc.annotation.config;

import com.atlwj.demo.ioc.annotation.entity.Person;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScans(
		@ComponentScan(value = "com.atlwj.demo.ioc.annotation")
)
public class Config {

	@Bean
	public Person person(){
		return new Person("lengweijian",27);
	}


	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		context.start();
		Person person = (Person) context.getBean("person");
		System.out.println(person.toString());
	}
}
