/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean} that evaluates a property path on a given target object.
 *
 * <p>The target object can be specified directly or via a bean name.
 *
 * <p>Usage examples:
 *
 * <pre class="code">&lt;!-- target bean to be referenced by name --&gt;
 * &lt;bean id="tb" class="org.springframework.beans.TestBean" singleton="false"&gt;
 *   &lt;property name="age" value="10"/&gt;
 *   &lt;property name="spouse"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="11"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 12, which is the value of property 'age' of the inner bean --&gt;
 * &lt;bean id="propertyPath1" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetObject"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="12"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="propertyPath" value="age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 11, which is the value of property 'spouse.age' of bean 'tb' --&gt;
 * &lt;bean id="propertyPath2" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetBeanName" value="tb"/&gt;
 *   &lt;property name="propertyPath" value="spouse.age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;bean id="tb.age" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"/&gt;</pre>
 *
 * <p>If you are using Spring 2.0 and XML Schema support in your configuration file(s),
 * you can also use the following style of configuration for property path access.
 * (See also the appendix entitled 'XML Schema-based configuration' in the Spring
 * reference manual for more examples.)
 *
 * <pre class="code"> &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;util:property-path id="name" path="testBean.age"/&gt;</pre>
 *
 * Thanks to Matthias Ernst for the suggestion and initial prototype!
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 * @see #setTargetObject
 * @see #setTargetBeanName
 * @see #setPropertyPath
 */
public class PropertyPathFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PropertyPathFactoryBean.class);

	private @Nullable BeanWrapper targetBeanWrapper;

	@SuppressWarnings("NullAway.Init")
	private String targetBeanName;

	private @Nullable String propertyPath;

	private @Nullable Class<?> resultType;

	@SuppressWarnings("NullAway.Init")
	private String beanName;

	private @Nullable BeanFactory beanFactory;


	/**
	 * Specify a target object to apply the property path to.
	 * Alternatively, specify a target bean name.
	 * @param targetObject a target object, for example a bean reference
	 * or an inner bean
	 * @see #setTargetBeanName
	 */
	public void setTargetObject(Object targetObject) {
		this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(targetObject);
	}

	/**
	 * Specify the name of a target bean to apply the property path to.
	 * Alternatively, specify a target object directly.
	 * @param targetBeanName the bean name to be looked up in the
	 * containing bean factory (for example, "testBean")
	 * @see #setTargetObject
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = StringUtils.trimAllWhitespace(targetBeanName);
	}

	/**
	 * Specify the property path to apply to the target.
	 * @param propertyPath the property path, potentially nested
	 * (for example, "age" or "spouse.age")
	 */
	public void setPropertyPath(String propertyPath) {
		this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
	}

	/**
	 * Specify the type of the result from evaluating the property path.
	 * <p>Note: This is not necessary for directly specified target objects
	 * or singleton target beans, where the type can be determined through
	 * introspection. Just specify this in case of a prototype target,
	 * provided that you need matching by type (for example, for autowiring).
	 * @param resultType the result type, for example "java.lang.Integer"
	 */
	public void setResultType(Class<?> resultType) {
		this.resultType = resultType;
	}

	/**
	 * The bean name of this PropertyPathFactoryBean will be interpreted
	 * as "beanName.property" pattern, if neither "targetObject" nor
	 * "targetBeanName" nor "propertyPath" have been specified.
	 * This allows for concise bean definitions with just an id/name.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;

		if (this.targetBeanWrapper != null && this.targetBeanName != null) {
			throw new IllegalArgumentException("Specify either 'targetObject' or 'targetBeanName', not both");
		}

		if (this.targetBeanWrapper == null && this.targetBeanName == null) {
			if (this.propertyPath != null) {
				throw new IllegalArgumentException(
						"Specify 'targetObject' or 'targetBeanName' in combination with 'propertyPath'");
			}

			// No other properties specified: check bean name.
			int dotIndex = (this.beanName != null ? this.beanName.indexOf('.') : -1);
			if (dotIndex == -1) {
				throw new IllegalArgumentException(
						"Neither 'targetObject' nor 'targetBeanName' specified, and PropertyPathFactoryBean " +
						"bean name '" + this.beanName + "' does not follow 'beanName.property' syntax");
			}
			this.targetBeanName = this.beanName.substring(0, dotIndex);
			this.propertyPath = this.beanName.substring(dotIndex + 1);
		}

		else if (this.propertyPath == null) {
			// either targetObject or targetBeanName specified
			throw new IllegalArgumentException("'propertyPath' is required");
		}

		if (this.targetBeanWrapper == null && this.beanFactory.isSingleton(this.targetBeanName)) {
			// Eagerly fetch singleton target bean, and determine result type.
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
			this.resultType = this.targetBeanWrapper.getPropertyType(this.propertyPath);
		}
	}


	@Override
	public @Nullable Object getObject() throws BeansException {
		BeanWrapper target = this.targetBeanWrapper;
		if (target != null) {
			if (logger.isWarnEnabled() && this.targetBeanName != null &&
					this.beanFactory instanceof ConfigurableBeanFactory cbf &&
					cbf.isCurrentlyInCreation(this.targetBeanName)) {
				logger.warn("Target bean '" + this.targetBeanName + "' is still in creation due to a circular " +
						"reference - obtained value for property '" + this.propertyPath + "' may be outdated!");
			}
		}
		else {
			// Fetch prototype target bean...
			Assert.state(this.beanFactory != null, "No BeanFactory available");
			Assert.state(this.targetBeanName != null, "No target bean name specified");
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			target = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		}
		Assert.state(this.propertyPath != null, "No property path specified");
		return target.getPropertyValue(this.propertyPath);
	}

	@Override
	public @Nullable Class<?> getObjectType() {
		return this.resultType;
	}

	/**
	 * While this FactoryBean will often be used for singleton targets,
	 * the invoked getters for the property path might return a new object
	 * for each call, so we have to assume that we're not returning the
	 * same object for each {@link #getObject()} call.
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}

}
