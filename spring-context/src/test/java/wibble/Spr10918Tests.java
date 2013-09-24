
package wibble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class Spr10918Tests {

	@Test
	public void invokesRegistrarOnlyOnce() {
		final AnnotationConfigApplicationContext myContext = new AnnotationConfigApplicationContext();
		myContext.scan(getClass().getPackage().getName());
		myContext.refresh();
		assertThat(TestImport.invocations, is(1));
		myContext.close();
	}

	@Import(TestImport.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnotherImport {
	}

	@Configuration
	@AnotherImport
	public static class TestConfiguration {
	}

	public static class TestImport implements ImportBeanDefinitionRegistrar {

		static int invocations = 0;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata anImport,
				BeanDefinitionRegistry aRegistry) {
			invocations++;
		}
	}
}
