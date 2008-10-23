/*
 * Copyright 2002-2005 the original author or authors.
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

/**
 * Extends the <code>MBeanInfoAssembler</code> to add autodetection logic.
 * Implementations of this interface are given the opportunity by the
 * <code>MBeanExporter</code> to include additional beans in the registration process.
 *
 * <p>The exact mechanism for deciding which beans to include is left to
 * implementing classes.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface AutodetectCapableMBeanInfoAssembler extends MBeanInfoAssembler {

	/**
	 * Indicate whether a particular bean should be included in the registration
	 * process, if it is not specified in the <code>beans</code> map of the
	 * <code>MBeanExporter</code>.
	 * @param beanClass the class of the bean (might be a proxy class)
	 * @param beanName the name of the bean in the bean factory
	 */
	boolean includeBean(Class beanClass, String beanName);

}
