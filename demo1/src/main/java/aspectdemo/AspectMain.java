package aspectdemo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;

public class AspectMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfigAspect.class);
		System.out.println(Arrays.asList(context.getBeanFactory().getBeanDefinitionNames()).toString().replaceAll(",", "\n"));
		PersonService personService = context.getBean(PersonService.class);
		personService.getPersonName();
		personService.setPersonName("KAKA");
		context.close();
	}
}
