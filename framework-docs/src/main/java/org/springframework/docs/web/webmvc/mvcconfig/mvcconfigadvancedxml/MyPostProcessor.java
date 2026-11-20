/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigadvancedxml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

// tag::snippet[]
@Component
public class MyPostProcessor implements BeanPostProcessor {

	public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
		// ...
		return bean;
	}
}
// end::snippet[]
