/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * A public {@link FactoryBean} that produces an array of objects.
 *
 * @author Stephane Nicoll
 */
public class SimpleBeanArrayFactoryBean extends AbstractFactoryBean<SimpleBean[]> {

	@Override
	public Class<?> getObjectType() {
		return SimpleBean[].class;
	}

	@Override
	protected SimpleBean[] createInstance() throws Exception {
		return new SimpleBean[0];
	}

}
