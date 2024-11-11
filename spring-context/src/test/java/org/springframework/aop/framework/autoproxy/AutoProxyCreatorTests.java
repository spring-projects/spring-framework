/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.IndexedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 09.12.2003
 */
class AutoProxyCreatorTests {

	@Test
	void beanNameAutoProxyCreator() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testInterceptor", TestInterceptor.class);

		RootBeanDefinition proxyCreator = new RootBeanDefinition(BeanNameAutoProxyCreator.class);
		proxyCreator.getPropertyValues().add("interceptorNames", "testInterceptor");
		proxyCreator.getPropertyValues().add("beanNames", "singletonToBeProxied,innerBean,singletonFactoryToBeProxied");
		sac.getDefaultListableBeanFactory().registerBeanDefinition("beanNameAutoProxyCreator", proxyCreator);

		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		RootBeanDefinition innerBean = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().add("spouse", new BeanDefinitionHolder(innerBean, "innerBean"));
		sac.getDefaultListableBeanFactory().registerBeanDefinition("singletonToBeProxied", bd);

		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);
		sac.registerSingleton("autowiredIndexedTestBean", IndexedTestBean.class);

		sac.refresh();

		MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		assertThat(Proxy.isProxyClass(messageSource.getClass())).isFalse();
		assertThat(Proxy.isProxyClass(singletonToBeProxied.getClass())).isTrue();
		assertThat(Proxy.isProxyClass(singletonToBeProxied.getSpouse().getClass())).isTrue();

		// test whether autowiring succeeded with auto proxy creation
		assertThat(singletonToBeProxied.getNestedIndexedBean()).isEqualTo(sac.getBean("autowiredIndexedTestBean"));

		TestInterceptor ti = (TestInterceptor) sac.getBean("testInterceptor");
		// already 2: getSpouse + getNestedIndexedBean calls above
		assertThat(ti.nrOfInvocations).isEqualTo(2);
		singletonToBeProxied.getName();
		singletonToBeProxied.getSpouse().getName();
		assertThat(ti.nrOfInvocations).isEqualTo(5);

		ITestBean tb = (ITestBean) sac.getBean("singletonFactoryToBeProxied");
		assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
		assertThat(ti.nrOfInvocations).isEqualTo(5);
		tb.getAge();
		assertThat(ti.nrOfInvocations).isEqualTo(6);

		ITestBean tb2 = (ITestBean) sac.getBean("singletonFactoryToBeProxied");
		assertThat(tb2).isSameAs(tb);
		assertThat(ti.nrOfInvocations).isEqualTo(6);
		tb2.getAge();
		assertThat(ti.nrOfInvocations).isEqualTo(7);

		sac.close();
	}

	@Test
	void beanNameAutoProxyCreatorWithFactoryBeanProxy() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testInterceptor", TestInterceptor.class);

		RootBeanDefinition proxyCreator = new RootBeanDefinition(BeanNameAutoProxyCreator.class);
		proxyCreator.getPropertyValues().add("interceptorNames", "testInterceptor");
		proxyCreator.getPropertyValues().add("beanNames", "singletonToBeProxied,&singletonFactoryToBeProxied");
		sac.getDefaultListableBeanFactory().registerBeanDefinition("beanNameAutoProxyCreator", proxyCreator);

		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		sac.getDefaultListableBeanFactory().registerBeanDefinition("singletonToBeProxied", bd);

		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);

		sac.refresh();

		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		assertThat(Proxy.isProxyClass(singletonToBeProxied.getClass())).isTrue();

		TestInterceptor ti = (TestInterceptor) sac.getBean("testInterceptor");
		int initialNr = ti.nrOfInvocations;
		singletonToBeProxied.getName();
		assertThat(ti.nrOfInvocations).isEqualTo((initialNr + 1));

		FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
		assertThat(Proxy.isProxyClass(factory.getClass())).isTrue();
		TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertThat(AopUtils.isAopProxy(tb)).isFalse();
		assertThat(ti.nrOfInvocations).isEqualTo((initialNr + 3));
		tb.getAge();
		assertThat(ti.nrOfInvocations).isEqualTo((initialNr + 3));

		sac.close();
	}

	@Test
	void customAutoProxyCreator() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("autoProxyCreator", TestAutoProxyCreator.class);
		sac.registerSingleton("noInterfaces", NoInterfaces.class);
		sac.registerSingleton("containerCallbackInterfacesOnly", ContainerCallbackInterfacesOnly.class);
		sac.registerSingleton("singletonNoInterceptor", TestBean.class);
		sac.registerSingleton("singletonToBeProxied", TestBean.class);
		sac.registerPrototype("prototypeToBeProxied", TestBean.class);
		sac.refresh();

		MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
		NoInterfaces noInterfaces = (NoInterfaces) sac.getBean("noInterfaces");
		ContainerCallbackInterfacesOnly containerCallbackInterfacesOnly =
				(ContainerCallbackInterfacesOnly) sac.getBean("containerCallbackInterfacesOnly");
		ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
		assertThat(AopUtils.isCglibProxy(messageSource)).isFalse();
		assertThat(AopUtils.isCglibProxy(noInterfaces)).isTrue();
		assertThat(AopUtils.isCglibProxy(containerCallbackInterfacesOnly)).isTrue();
		assertThat(AopUtils.isCglibProxy(singletonNoInterceptor)).isTrue();
		assertThat(AopUtils.isCglibProxy(singletonToBeProxied)).isTrue();
		assertThat(AopUtils.isCglibProxy(prototypeToBeProxied)).isTrue();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		singletonNoInterceptor.getName();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		singletonToBeProxied.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
		prototypeToBeProxied.getSpouse();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithFallbackToTargetClass() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("autoProxyCreator", FallbackTestAutoProxyCreator.class);
		sac.registerSingleton("noInterfaces", NoInterfaces.class);
		sac.registerSingleton("containerCallbackInterfacesOnly", ContainerCallbackInterfacesOnly.class);
		sac.registerSingleton("singletonNoInterceptor", TestBean.class);
		sac.registerSingleton("singletonToBeProxied", TestBean.class);
		sac.registerPrototype("prototypeToBeProxied", TestBean.class);
		sac.refresh();

		MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
		NoInterfaces noInterfaces = (NoInterfaces) sac.getBean("noInterfaces");
		ContainerCallbackInterfacesOnly containerCallbackInterfacesOnly =
				(ContainerCallbackInterfacesOnly) sac.getBean("containerCallbackInterfacesOnly");
		ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
		assertThat(AopUtils.isCglibProxy(messageSource)).isFalse();
		assertThat(AopUtils.isCglibProxy(noInterfaces)).isTrue();
		assertThat(AopUtils.isCglibProxy(containerCallbackInterfacesOnly)).isTrue();
		assertThat(AopUtils.isCglibProxy(singletonNoInterceptor)).isFalse();
		assertThat(AopUtils.isCglibProxy(singletonToBeProxied)).isFalse();
		assertThat(AopUtils.isCglibProxy(prototypeToBeProxied)).isFalse();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		singletonNoInterceptor.getName();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		singletonToBeProxied.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
		prototypeToBeProxied.getSpouse();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithFallbackToDynamicProxy() {
		StaticApplicationContext sac = new StaticApplicationContext();

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("proxyFactoryBean", "false");
		sac.registerSingleton("autoProxyCreator", IntroductionTestAutoProxyCreator.class, pvs);

		sac.registerSingleton("noInterfaces", NoInterfaces.class);
		sac.registerSingleton("containerCallbackInterfacesOnly", ContainerCallbackInterfacesOnly.class);
		sac.registerSingleton("singletonNoInterceptor", CustomProxyFactoryBean.class);
		sac.registerSingleton("singletonToBeProxied", CustomProxyFactoryBean.class);
		sac.registerPrototype("prototypeToBeProxied", SpringProxyFactoryBean.class);

		sac.refresh();

		MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
		NoInterfaces noInterfaces = (NoInterfaces) sac.getBean("noInterfaces");
		ContainerCallbackInterfacesOnly containerCallbackInterfacesOnly =
				(ContainerCallbackInterfacesOnly) sac.getBean("containerCallbackInterfacesOnly");
		ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
		assertThat(AopUtils.isCglibProxy(messageSource)).isFalse();
		assertThat(AopUtils.isCglibProxy(noInterfaces)).isTrue();
		assertThat(AopUtils.isCglibProxy(containerCallbackInterfacesOnly)).isTrue();
		assertThat(AopUtils.isCglibProxy(singletonNoInterceptor)).isFalse();
		assertThat(AopUtils.isCglibProxy(singletonToBeProxied)).isFalse();
		assertThat(AopUtils.isCglibProxy(prototypeToBeProxied)).isFalse();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		singletonNoInterceptor.getName();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		singletonToBeProxied.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
		prototypeToBeProxied.getSpouse();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(4);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithPackageVisibleMethod() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("autoProxyCreator", TestAutoProxyCreator.class);
		sac.registerSingleton("packageVisibleMethodToBeProxied", PackageVisibleMethod.class);
		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		PackageVisibleMethod tb = (PackageVisibleMethod) sac.getBean("packageVisibleMethodToBeProxied");
		assertThat(AopUtils.isCglibProxy(tb)).isTrue();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		tb.doSomething();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithFactoryBean() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("autoProxyCreator", TestAutoProxyCreator.class);
		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);
		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(factory)).isTrue();

		TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(tb)).isTrue();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
		tb.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(3);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithFactoryBeanAndPrototype() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("autoProxyCreator", TestAutoProxyCreator.class);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("singleton", "false");
		sac.registerSingleton("prototypeFactoryToBeProxied", DummyFactory.class, pvs);

		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> prototypeFactory = (FactoryBean<?>) sac.getBean("&prototypeFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(prototypeFactory)).isTrue();
		TestBean tb = (TestBean) sac.getBean("prototypeFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(tb)).isTrue();

		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
		tb.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(3);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithFactoryBeanAndProxyObjectOnly() {
		StaticApplicationContext sac = new StaticApplicationContext();

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("proxyFactoryBean", "false");
		sac.registerSingleton("autoProxyCreator", TestAutoProxyCreator.class, pvs);

		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);

		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
		assertThat(AopUtils.isAopProxy(factory)).isFalse();

		TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(tb)).isTrue();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
		tb.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);

		TestBean tb2 = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertThat(tb2).isSameAs(tb);
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
		tb2.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);

		sac.close();
	}

	@Test
	void autoProxyCreatorWithFactoryBeanAndProxyFactoryBeanOnly() {
		StaticApplicationContext sac = new StaticApplicationContext();

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("proxyObject", "false");
		sac.registerSingleton("autoProxyCreator", TestAutoProxyCreator.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("singleton", "false");
		sac.registerSingleton("prototypeFactoryToBeProxied", DummyFactory.class, pvs);

		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("autoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> prototypeFactory = (FactoryBean<?>) sac.getBean("&prototypeFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(prototypeFactory)).isTrue();
		TestBean tb = (TestBean) sac.getBean("prototypeFactoryToBeProxied");
		assertThat(AopUtils.isCglibProxy(tb)).isFalse();

		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
		tb.getAge();
		assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);

		sac.close();
	}


	@SuppressWarnings("serial")
	public static class TestAutoProxyCreator extends AbstractAutoProxyCreator {

		private boolean proxyFactoryBean = true;

		private boolean proxyObject = true;

		public TestInterceptor testInterceptor = new TestInterceptor();

		public TestAutoProxyCreator() {
			setProxyTargetClass(true);
			setOrder(0);
		}

		public void setProxyFactoryBean(boolean proxyFactoryBean) {
			this.proxyFactoryBean = proxyFactoryBean;
		}

		public void setProxyObject(boolean proxyObject) {
			this.proxyObject = proxyObject;
		}

		@Override
		@Nullable
		protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String name, @Nullable TargetSource customTargetSource) {
			if (StaticMessageSource.class.equals(beanClass)) {
				return DO_NOT_PROXY;
			}
			else if (name.endsWith("ToBeProxied")) {
				boolean isFactoryBean = FactoryBean.class.isAssignableFrom(beanClass);
				if ((this.proxyFactoryBean && isFactoryBean) || (this.proxyObject && !isFactoryBean)) {
					return getAdvicesAndAdvisors();
				}
				else {
					return DO_NOT_PROXY;
				}
			}
			else {
				return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
			}
		}

		protected Object[] getAdvicesAndAdvisors() {
			return new Object[] {this.testInterceptor};
		}
	}


	@SuppressWarnings("serial")
	public static class FallbackTestAutoProxyCreator extends TestAutoProxyCreator {

		public FallbackTestAutoProxyCreator() {
			setProxyTargetClass(false);
		}
	}


	@SuppressWarnings("serial")
	public static class IntroductionTestAutoProxyCreator extends TestAutoProxyCreator {

		@Override
		protected Object[] getAdvicesAndAdvisors() {
			DefaultIntroductionAdvisor advisor = new DefaultIntroductionAdvisor(this.testInterceptor);
			advisor.addInterface(Serializable.class);
			return new Object[] {this.testInterceptor, advisor};
		}
	}


	/**
	 * Interceptor that counts the number of non-finalize method calls.
	 */
	public static class TestInterceptor implements MethodInterceptor {

		public int nrOfInvocations = 0;

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			if (!invocation.getMethod().getName().equals("finalize")) {
				this.nrOfInvocations++;
			}
			return invocation.proceed();
		}
	}


	public static class NoInterfaces {
	}


	@SuppressWarnings("serial")
	public static class ContainerCallbackInterfacesOnly  // as well as an empty marker interface
			implements BeanFactoryAware, ApplicationContextAware, InitializingBean, DisposableBean, Serializable {

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
		}

		@Override
		public void afterPropertiesSet() {
		}

		@Override
		public void destroy() {
		}
	}


	public static class CustomProxyFactoryBean implements FactoryBean<ITestBean> {

		private final TestBean tb = new TestBean();

		@Override
		public ITestBean getObject() {
			return (ITestBean) Proxy.newProxyInstance(CustomProxyFactoryBean.class.getClassLoader(), new Class<?>[]{ITestBean.class},
					(proxy, method, args) -> ReflectionUtils.invokeMethod(method, tb, args));
		}

		@Override
		public Class<?> getObjectType() {
			return ITestBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}
	}


	public static class SpringProxyFactoryBean implements FactoryBean<ITestBean> {

		private final TestBean tb = new TestBean();

		@Override
		public ITestBean getObject() {
			return ProxyFactory.getProxy(ITestBean.class, new SingletonTargetSource(tb));
		}

		@Override
		public Class<?> getObjectType() {
			return ITestBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}
	}

}
