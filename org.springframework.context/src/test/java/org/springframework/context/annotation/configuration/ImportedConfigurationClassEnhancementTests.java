package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
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

	
	@Ignore @Test
	public void autowiredConfigClassIsEnhancedWhenImported() {
		autowiredConfigClassIsEnhanced(ConfigThatImports.class);
	}

	@Test
	public void autowiredConfigClassIsEnhancedWhenRegisteredViaConstructor() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}
	
	private void autowiredConfigClassIsEnhanced(Class<?>... configClasses) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		assertTrue(AopUtils.isCglibProxyClass(config.autowiredConfig.getClass()));
	}
	
	
	@Ignore @Test
	public void autowiredConfigClassBeanMethodsRespectScopingWhenImported() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatImports.class);
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
		assertThat(testBean1, sameInstance(testBean2));
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
class ConfigThatImports extends Config { }

@Configuration
class ConfigThatDoesNotImport extends Config { }
