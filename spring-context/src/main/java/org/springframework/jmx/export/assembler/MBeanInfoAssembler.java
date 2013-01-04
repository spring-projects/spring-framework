/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.export.assembler;

import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanInfo;

/**
 * Interface to be implemented by all classes that can
 * create management interface metadata for a managed resource.
 *
 * <p>Used by the {@code MBeanExporter} to generate the management
 * interface for any bean that is not an MBean.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface MBeanInfoAssembler {

	/**
	 * Create the ModelMBeanInfo for the given managed resource.
	 * @param managedBean the bean that will be exposed (might be an AOP proxy)
	 * @param beanKey the key associated with the managed bean
	 * @return the ModelMBeanInfo metadata object
	 * @throws JMException in case of errors
	 */
	ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException;

}
