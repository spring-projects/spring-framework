/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation4;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.BeanAge;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Class used to test the functionality of factory method bean definitions
 * declared inside a Spring component class.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
@Component
public class FactoryMethodComponent {

	private int i;

	public static TestBean nullInstance() {
		return null;
	}

	@Bean @Qualifier("public")
	public TestBean publicInstance() {
		return new TestBean("publicInstance");
	}

	// to be ignored
	public TestBean publicInstance(boolean doIt) {
		return new TestBean("publicInstance");
	}

	@Bean @BeanAge(1)
	protected TestBean protectedInstance(@Qualifier("public") TestBean spouse, @Value("#{privateInstance.age}") String country) {
		TestBean tb = new TestBean("protectedInstance", 1);
		tb.setSpouse(tb);
		tb.setCountry(country);
		return tb;
	}

	@Bean @Scope("prototype")
	private TestBean privateInstance() {
		return new TestBean("privateInstance", i++);
	}

	@Bean @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public TestBean requestScopedInstance() {
		return new TestBean("requestScopedInstance", 3);
	}

	@Bean
	public DependencyBean secondInstance() {
		return new DependencyBean();
	}

}
