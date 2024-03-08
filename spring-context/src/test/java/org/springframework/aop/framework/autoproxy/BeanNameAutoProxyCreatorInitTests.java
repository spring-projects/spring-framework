/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.lang.Nullable;

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
