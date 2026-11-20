/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @author Sam Brannen
 */
class BeanNameAutoProxyCreatorInitTests {

	@Test
	void ignoreAdvisorThatIsCurrentlyInCreation() {
		String path = getClass().getSimpleName() + "-context.xml";
		try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(path, getClass())) {
			Pet pet = ctx.getBean(Pet.class);
			assertThat(pet.getName()).isEqualTo("Simba");
			pet.setName("Tiger");
			assertThat(pet.getName()).isEqualTo("Tiger");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> pet.setName(null))
				.withMessage("Null argument at position 0");
		}
	}

}


class NullChecker implements MethodBeforeAdvice {

	@Override
	public void before(Method method, Object[] args, @Nullable Object target) {
		check(args);
	}

	private void check(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				throw new IllegalArgumentException("Null argument at position " + i);
			}
		}
	}

}
