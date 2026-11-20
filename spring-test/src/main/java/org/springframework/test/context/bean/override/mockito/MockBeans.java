/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;

/**
 * Beans created using Mockito.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 */
class MockBeans {

	private final List<Object> beans = new ArrayList<>();


	void add(Object bean) {
		this.beans.add(bean);
	}

	/**
	 * Reset all Mockito beans configured with the supplied {@link MockReset} strategy.
	 * <p>No mocks will be reset if the supplied strategy is {@link MockReset#NONE}.
	 */
	void resetAll(MockReset reset) {
		if (reset != MockReset.NONE) {
			for (Object bean : this.beans) {
				if (reset == MockReset.get(bean)) {
					Mockito.reset(bean);
				}
			}
		}
	}

}
