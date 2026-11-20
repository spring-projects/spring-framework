/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.easymock;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;

/**
 * @author Sam Brannen
 * @since 6.2
 */
class EasyMockBeans {

	private final List<Object> beans = new ArrayList<>();

	void add(Object bean) {
		this.beans.add(bean);
	}

	void resetAll() {
		this.beans.forEach(EasyMock::reset);
	}

}
