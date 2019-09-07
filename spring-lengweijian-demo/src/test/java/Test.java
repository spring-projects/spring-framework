import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Configuration
@Import(value = Test01.class)
public class Test {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Test.class);
		System.out.println(context.getBean(Test01.class).getList());
	}


	@org.junit.Test
	public void test(){
		Properties properties = System.getProperties();
		for (String stringPropertyName : properties.stringPropertyNames()) {
			System.out.println(stringPropertyName);
		}
	}
}
