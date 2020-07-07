package test;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextTests {

	@Test
	public void testOneIntArg() {
		ApplicationContext aa = new AnnotationConfigApplicationContext(ContextTests.class);
		Person person =aa.getBean(Person.class);
		person.print();
	}

	@Bean
	public Person getPerson(){
		return new Person();
	}


}

class Person{
	public void print() {
		System.out.println("Person" + this.hashCode());
	}
}