/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.support;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * The bean factory for EasyMock mocks. Assumes that spring context has a bean with name "mocksControl" which will be
 * used to create mocks; otherwise is customizable via property or factory will fallback to creating a separate control
 * per mock. Example of usage:
 * 
 * <pre>
 * &lt;bean name="myService" class="org.springframework.test.context.support.EasyMockBeanFactory"
 * 	p:mockInterface="org.company.api.MyService"
 * />
 * 
 * &lt;bean name="myDao" class="org.springframework.test.context.support.EasyMockBeanFactory"
 * 	p:mockInterface="org.company.api.Dao"
 * 	p:mocksControl-ref="mocksCtrl"
 * />
 * </pre>
 * 
 * Then test class may look like:
 * 
 * <pre>
 * import static org.junit.Assert.assertEquals;
 * import static org.junit.Assert.assertNull;
 * 
 * import javax.annotation.Resource;
 * 
 * import org.easymock.EasyMock;
 * import org.easymock.IMocksControl;
 * import org.junit.Before;
 * import org.junit.Test;
 * import org.junit.runner.RunWith;
 * import org.springframework.beans.factory.annotation.Autowired;
 * import org.springframework.test.context.ContextConfiguration;
 * import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 * 
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;ContextConfiguration(&quot;classpath:/org/company/test-context.xml&quot;)
 * public class MyIntegrationTest {
 * 	&#064;Autowired
 * 	private MyService myService;
 * 
 * 	// This is a mock:
 * 	&#064;Autowired
 * 	private Dao dao;
 * 
 * 	&#064;Resource
 * 	protected IMocksControl mocksControl;
 * 
 * 	&#064;Before
 * 	public void reset() {
 * 		mocksControl.reset();
 * 	}
 * 
 * 	&#064;Test
 * 	public void testGetPublicationNoSearchHit() {
 * 		EasyMock.expect(dao.getItemByKey(999)).andReturn(null);
 * 
 * 		mocksControl.replay();
 * 
 * 		assertNull(myService.getOrCreateItem());
 * 
 * 		mocksControl.verify();
 * 	}
 * }
 * </pre>
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class EasyMockBeanFactory<T> implements FactoryBean<T>, InitializingBean, BeanFactoryAware {

	private IMocksControl mocksControl;

	private Class<T> mockClass;

	private T mock;

	private BeanFactory beanFactory;

	static final String DEFAULT_MOCKS_CONTROL_BEAN_NAME = "mocksControl";

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public T getObject() {
		return mock;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<?> getObjectType() {
		return mockClass;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Sets {@link IMocksControl} on this factory. This is necessary when you want to share the same
	 * {@link IMocksControl} among all mocks.
	 */
	public void setMocksControl(IMocksControl mocksControl) {
		this.mocksControl = mocksControl;
	}

	/**
	 * Set the class to be mocked.
	 */
	public void setMockClass(Class<T> mockClass) {
		this.mockClass = mockClass;
	}

	/**
	 * The same as {@link #setMockClass(Class)} but provided for better expressibility.
	 */
	public void setMockInterface(Class<T> mockClass) {
		this.mockClass = mockClass;
	}

	/**
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws FatalBeanException {
		Assert.notNull(mockClass, "The class/interface to mock must be defined");

		if (mocksControl == null) {
			// Check the presence of a bean with default name:
			mocksControl = beanFactory.getBean(DEFAULT_MOCKS_CONTROL_BEAN_NAME, IMocksControl.class);

			if (mocksControl == null) {
				// Fallback to separate control per mock:
				mocksControl = EasyMock.createControl();
			}
		}

		mock = mocksControl.createMock(mockClass);
	}
}
