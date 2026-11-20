/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.wiring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassNameBeanWiringInfoResolver}.
 *
 * @author Rick Evans
 */
class ClassNameBeanWiringInfoResolverTests {

	@Test
	void resolveWiringInfoWithNullBeanInstance() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ClassNameBeanWiringInfoResolver().resolveWiringInfo(null));
	}

	@Test
	void resolveWiringInfo() {
		ClassNameBeanWiringInfoResolver resolver = new ClassNameBeanWiringInfoResolver();
		Long beanInstance = 1L;
		BeanWiringInfo info = resolver.resolveWiringInfo(beanInstance);
		assertThat(info).isNotNull();
		assertThat(info.getBeanName()).as("Not resolving bean name to the class name of the supplied bean instance as per class contract.").isEqualTo(beanInstance.getClass().getName());
	}

}
