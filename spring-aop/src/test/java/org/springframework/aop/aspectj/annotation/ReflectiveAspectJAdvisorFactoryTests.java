/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

/**
 * Tests for {@link ReflectiveAspectJAdvisorFactory}.
 *
 * <p>Tests are inherited: we only set the test fixture here.
 *
 * @author Rod Johnson
 * @since 2.0
 */
class ReflectiveAspectJAdvisorFactoryTests extends AbstractAspectJAdvisorFactoryTests {

	@Override
	protected AspectJAdvisorFactory getAdvisorFactory() {
		return new ReflectiveAspectJAdvisorFactory();
	}

}
