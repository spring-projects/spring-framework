package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import test.beans.TestBean;

/**
 * Unit tests cornering the bug exposed in SPR-6779.
 *
 * @author Chris Beams
 */
public class ImportedConfigurationClassEnhancementTests {

	
	@Test
	public void autowiredConfigClassIsEnhancedWhenImported() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesImport.class);
	}

	@Test
	public void autowiredConfigClassIsEnhancedWhenRegisteredViaConstructor() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}
	
	private void autowiredConfigClassIsEnhanced(Class<?>... configClasses) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		assertTrue("autowired config class has not been enhanced",
				AopUtils.isCglibProxyClass(config.autowiredConfig.getClass()));
	}
	
	
	@Test
	public void autowiredConfigClassBeanMethodsRespectScopingWhenImported() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesImport.class);
	}
	
	@Test
	public void autowiredConfigClassBeanMethodsRespectScopingWhenRegisteredViaConstructor() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}
	
	private void autowiredConfigClassBeanMethodsRespectScoping(Class<?>... configClasses) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		TestBean testBean1 = config.autowiredConfig.testBean();
		TestBean testBean2 = config.autowiredConfig.testBean();
		assertThat("got two distinct instances of testBean when singleton scoping was expected",
				testBean1, sameInstance(testBean2));
	}
	
}

@Configuration
class ConfigToBeAutowired {
	public @Bean TestBean testBean() {
		return new TestBean();
	}
}

class Config {
	@Autowired ConfigToBeAutowired autowiredConfig;
}

@Import(ConfigToBeAutowired.class)
@Configuration
class ConfigThatDoesImport extends Config { }

@Configuration
class ConfigThatDoesNotImport extends Config { }
