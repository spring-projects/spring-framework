package test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.service.PersonService;

@Configuration
public class TestAnnotationContext {

	public static void main(String[] args) {
		ApplicationContext aa = new AnnotationConfigApplicationContext(TestAnnotationContext.class);
		PersonService person =aa.getBean(PersonService.class);
		person.print();
	}

	@Bean
	public PersonService getPerson(){
		return new PersonService();
	}
}

