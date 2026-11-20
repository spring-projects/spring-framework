/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Phillip Webb
 */
class EntityManagerFactoryBeanSupportTests extends AbstractEntityManagerFactoryBeanTests {

	@Test
	void hookIsCalled() {
		DummyEntityManagerFactoryBean demf = new DummyEntityManagerFactoryBean(mockEmf);

		demf.afterPropertiesSet();

		checkInvariants(demf);

		// Should trigger close method expected by EntityManagerFactory mock
		demf.destroy();

		verify(mockEmf).close();
	}

}
