/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

import org.springframework.beans.factory.FactoryBean;

/**
 * A public {@link FactoryBean} with a resolved generic for {@link GenericFactoryBean}.
 *
 * @author Stephane Nicoll
 */
public class SimpleBeanFactoryBean extends GenericFactoryBean<SimpleBean> {

	public SimpleBeanFactoryBean() {
		super(SimpleBean.class);
	}

}
