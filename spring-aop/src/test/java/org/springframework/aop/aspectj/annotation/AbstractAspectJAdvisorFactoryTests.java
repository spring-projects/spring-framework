/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.aop.DefaultLockable;
import test.aop.Lockable;
import test.aop.PerTargetAspect;
import test.aop.TwoAdviceAspect;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Abstract tests for {@link AspectJAdvisorFactory} implementations.
 *
 * <p>See subclasses for tests of concrete factories.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 */
abstract class AbstractAspectJAdvisorFactoryTests {

	/**
	 * To be overridden by concrete test subclasses.
	 */
	protected abstract AspectJAdvisorFactory getAdvisorFactory();


	@Test
	void rejectsPerCflowAspect() {
		assertThatExceptionOfType(AopConfigException.class)
			.isThrownBy(() -> getAdvisorFactory().getAdvisors(aspectInstanceFactory(new PerCflowAspect(), "someBean")))
			.withMessageContaining("PERCFLOW");
	}

	@Test
	void rejectsPerCflowBelowAspect() {
		assertThatExceptionOfType(AopConfigException.class)
			.isThrownBy(() -> getAdvisorFactory().getAdvisors(aspectInstanceFactory(new PerCflowBelowAspect(), "someBean")))
			.withMessageContaining("PERCFLOWBELOW");
	}

	@Test
	void perTargetAspect() throws Exception {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		TestBean itb = createProxy(target, TestBean.class,
				getAdvisorFactory().getAdvisors(aspectInstanceFactory(new PerTargetAspect(), "someBean")));
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		Advised advised = (Advised) itb;
		ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor sia =
				(ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertThat(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[3];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertThat(maaif.isMaterialized()).isFalse();

		// Check that the perclause pointcut is valid
		assertThat(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		assertThat(imapa.getPointcut()).isNotSameAs(imapa.getDeclaredPointcut());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(maaif.isMaterialized()).isTrue();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	void multiplePerTargetAspects() throws Exception {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);

		List<Advisor> advisors = new ArrayList<>();
		PerTargetAspect aspect1 = new PerTargetAspect();
		aspect1.count = 100;
		aspect1.setOrder(10);
		advisors.addAll(getAdvisorFactory().getAdvisors(aspectInstanceFactory(aspect1, "someBean1")));
		PerTargetAspect aspect2 = new PerTargetAspect();
		aspect2.setOrder(5);
		advisors.addAll(getAdvisorFactory().getAdvisors(aspectInstanceFactory(aspect2, "someBean2")));
		OrderComparator.sort(advisors);

		TestBean itb = createProxy(target, TestBean.class, advisors);
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	void multiplePerTargetAspectsWithOrderAnnotation() throws Exception {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);

		List<Advisor> advisors = new ArrayList<>();
		PerTargetAspectWithOrderAnnotation10 aspect1 = new PerTargetAspectWithOrderAnnotation10();
		aspect1.count = 100;
		advisors.addAll(getAdvisorFactory().getAdvisors(aspectInstanceFactory(aspect1, "someBean1")));
		PerTargetAspectWithOrderAnnotation5 aspect2 = new PerTargetAspectWithOrderAnnotation5();
		advisors.addAll(getAdvisorFactory().getAdvisors(aspectInstanceFactory(aspect2, "someBean2")));
		OrderComparator.sort(advisors);

		TestBean itb = createProxy(target, TestBean.class, advisors);
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	void perThisAspect() throws Exception {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		TestBean itb = createProxy(target, TestBean.class,
				getAdvisorFactory().getAdvisors(aspectInstanceFactory(new PerThisAspect(), "someBean")));
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		Advised advised = (Advised) itb;
		// Will be ExposeInvocationInterceptor, synthetic instantiation advisor, 2 method advisors
		assertThat(advised.getAdvisors()).hasSize(4);
		ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor sia =
				(ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertThat(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[2];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertThat(maaif.isMaterialized()).isFalse();

		// Check that the perclause pointcut is valid
		assertThat(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		assertThat(imapa.getPointcut()).isNotSameAs(imapa.getDeclaredPointcut());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(maaif.isMaterialized()).isTrue();

		assertThat(imapa.getDeclaredPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getAge"), null)).isTrue();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	void perTypeWithinAspect() throws Exception {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		PerTypeWithinAspectInstanceFactory aif = new PerTypeWithinAspectInstanceFactory();
		TestBean itb = createProxy(target, TestBean.class, getAdvisorFactory().getAdvisors(aif));
		assertThat(aif.getInstantiationCount()).as("No method calls").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must now apply").isEqualTo(0);

		Advised advised = (Advised) itb;
		// Will be ExposeInvocationInterceptor, synthetic instantiation advisor, 2 method advisors
		assertThat(advised.getAdvisors()).hasSize(4);
		ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor sia =
				(ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertThat(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[2];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertThat(maaif.isMaterialized()).isTrue();

		// Check that the perclause pointcut is valid
		assertThat(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		assertThat(imapa.getPointcut()).isNotSameAs(imapa.getDeclaredPointcut());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(maaif.isMaterialized()).isTrue();

		assertThat(imapa.getDeclaredPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getAge"), null)).isTrue();

		assertThat(itb.getAge()).as("Around advice must still apply").isEqualTo(1);
		assertThat(itb.getAge()).as("Around advice must still apply").isEqualTo(2);

		TestBean itb2 = createProxy(target, TestBean.class, getAdvisorFactory().getAdvisors(aif));
		assertThat(aif.getInstantiationCount()).isEqualTo(1);
		assertThat(itb2.getAge()).as("Around advice be independent for second instance").isEqualTo(0);
		assertThat(aif.getInstantiationCount()).isEqualTo(2);
	}

	@Test
	void namedPointcutAspectWithFQN() {
		namedPointcuts(new NamedPointcutAspectWithFQN());
	}

	@Test
	void namedPointcutAspectWithoutFQN() {
		namedPointcuts(new NamedPointcutAspectWithoutFQN());
	}

	@Test
	void namedPointcutFromAspectLibrary() {
		namedPointcuts(new NamedPointcutAspectFromLibrary());
	}

	@Test
	void namedPointcutFromAspectLibraryWithBinding() {
		TestBean target = new TestBean();
		ITestBean itb = createProxy(target, ITestBean.class,
				getAdvisorFactory().getAdvisors(aspectInstanceFactory(
						new NamedPointcutAspectFromLibraryWithBinding(), "someBean")));
		itb.setAge(10);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(20);
		assertThat(target.getAge()).isEqualTo(20);
	}

	private void namedPointcuts(Object aspectInstance) {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		ITestBean itb = createProxy(target, ITestBean.class,
			getAdvisorFactory().getAdvisors(aspectInstanceFactory(aspectInstance, "someBean")));
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(-1);
		assertThat(target.getAge()).isEqualTo(realAge);
	}

	@Test
	void bindingWithSingleArg() {
		TestBean target = new TestBean();
		ITestBean itb = createProxy(target, ITestBean.class,
			getAdvisorFactory().getAdvisors(aspectInstanceFactory(new BindingAspectWithSingleArg(), "someBean")));
		itb.setAge(10);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(20);
		assertThat(target.getAge()).isEqualTo(20);
	}

	@Test
	void bindingWithMultipleArgsDifferentlyOrdered() {
		ManyValuedArgs target = new ManyValuedArgs();
		ManyValuedArgs mva = createProxy(target, ManyValuedArgs.class,
			getAdvisorFactory().getAdvisors(aspectInstanceFactory(new ManyValuedArgs(), "someBean")));

		String a = "a";
		int b = 12;
		int c = 25;
		String d = "d";
		StringBuilder e = new StringBuilder("stringbuf");
		String expectedResult = a + b+ c + d + e;
		assertThat(mva.mungeArgs(a, b, c, d, e)).isEqualTo(expectedResult);
	}

	/**
	 * In this case the introduction will be made.
	 */
	@Test
	void introductionOnTargetNotImplementingInterface() {
		NotLockable notLockableTarget = new NotLockable();
		assertThat(notLockableTarget).isNotInstanceOf(Lockable.class);
		NotLockable notLockable1 = createProxy(notLockableTarget, NotLockable.class,
			getAdvisorFactory().getAdvisors(aspectInstanceFactory(new MakeLockable(), "someBean")));
		assertThat(notLockable1).isInstanceOf(Lockable.class);
		Lockable lockable = (Lockable) notLockable1;
		assertThat(lockable.locked()).isFalse();
		lockable.lock();
		assertThat(lockable.locked()).isTrue();

		NotLockable notLockable2Target = new NotLockable();
		NotLockable notLockable2 = createProxy(notLockable2Target, NotLockable.class,
			getAdvisorFactory().getAdvisors(aspectInstanceFactory(new MakeLockable(), "someBean")));
		assertThat(notLockable2).isInstanceOf(Lockable.class);
		Lockable lockable2 = (Lockable) notLockable2;
		assertThat(lockable2.locked()).isFalse();
		notLockable2.setIntValue(1);
		lockable2.lock();
		assertThatIllegalStateException().isThrownBy(() -> notLockable2.setIntValue(32));
		assertThat(lockable2.locked()).isTrue();
	}

	@Test
	void introductionAdvisorExcludedFromTargetImplementingInterface() {
		assertThat(AopUtils.findAdvisorsThatCanApply(
		getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new MakeLockable(), "someBean")),
		CannotBeUnlocked.class)).isEmpty();
		assertThat(AopUtils.findAdvisorsThatCanApply(getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new MakeLockable(),"someBean")), NotLockable.class)).hasSize(2);
	}

	@Test
	void introductionOnTargetImplementingInterface() {
		CannotBeUnlocked target = new CannotBeUnlocked();
		Lockable proxy = createProxy(target, CannotBeUnlocked.class,
				// Ensure that we exclude
				AopUtils.findAdvisorsThatCanApply(
					getAdvisorFactory().getAdvisors(aspectInstanceFactory(new MakeLockable(), "someBean")),
					CannotBeUnlocked.class));
		assertThat(proxy).isInstanceOf(Lockable.class);
		Lockable lockable = proxy;
		assertThat(lockable.locked()).as("Already locked").isTrue();
		lockable.lock();
		assertThat(lockable.locked()).as("Real target ignores locking").isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lockable.unlock());
	}

	@Test
	void introductionOnTargetExcludedByTypePattern() {
		ArrayList<Object> target = new ArrayList<>();
		List<?> proxy = createProxy(target, List.class,
				AopUtils.findAdvisorsThatCanApply(
					getAdvisorFactory().getAdvisors(aspectInstanceFactory(new MakeLockable(), "someBean")),
					List.class));
		assertThat(proxy).as("Type pattern must have excluded mixin").isNotInstanceOf(Lockable.class);
	}

	@Test
	void introductionBasedOnAnnotationMatch() { // gh-9980
		AnnotatedTarget target = new AnnotatedTargetImpl();
		List<Advisor> advisors = getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new MakeAnnotatedTypeModifiable(), "someBean"));
		Object proxy = createProxy(target, AnnotatedTarget.class, advisors);
		assertThat(proxy).isInstanceOf(Lockable.class);
		Lockable lockable = (Lockable) proxy;
		assertThat(lockable.locked()).isFalse();
		lockable.lock();
		assertThat(lockable.locked()).isTrue();
	}

	// TODO: Why does this test fail? It hasn't been run before, so it maybe never actually passed...
	@Test
	@Disabled
	void introductionWithArgumentBinding() {
		TestBean target = new TestBean();

		List<Advisor> advisors = getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new MakeITestBeanModifiable(), "someBean"));
		advisors.addAll(getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new MakeLockable(), "someBean")));

		Modifiable modifiable = (Modifiable) createProxy(target, ITestBean.class, advisors);
		assertThat(modifiable).isInstanceOf(Modifiable.class);
		Lockable lockable = (Lockable) modifiable;
		assertThat(lockable.locked()).isFalse();

		ITestBean itb = (ITestBean) modifiable;
		assertThat(modifiable.isModified()).isFalse();
		int oldAge = itb.getAge();
		itb.setAge(oldAge + 1);
		assertThat(modifiable.isModified()).isTrue();
		modifiable.acceptChanges();
		assertThat(modifiable.isModified()).isFalse();
		itb.setAge(itb.getAge());
		assertThat(modifiable.isModified()).as("Setting same value does not modify").isFalse();
		itb.setName("And now for something completely different");
		assertThat(modifiable.isModified()).isTrue();

		lockable.lock();
		assertThat(lockable.locked()).isTrue();
		assertThatIllegalStateException().as("Should be locked").isThrownBy(() -> itb.setName("Else"));
		lockable.unlock();
		itb.setName("Tony");
	}

	@Test
	void aspectMethodThrowsExceptionLegalOnSignature() {
		TestBean target = new TestBean();
		UnsupportedOperationException expectedException = new UnsupportedOperationException();
		List<Advisor> advisors = getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new ExceptionThrowingAspect(expectedException), "someBean"));
		assertThat(advisors).as("One advice method was found").hasSize(1);
		ITestBean itb = createProxy(target, ITestBean.class, advisors);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(itb::getAge);
	}

	// TODO document this behaviour.
	// Is it different AspectJ behaviour, at least for checked exceptions?
	@Test
	void aspectMethodThrowsExceptionIllegalOnSignature() {
		TestBean target = new TestBean();
		RemoteException expectedException = new RemoteException();
		List<Advisor> advisors = getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(new ExceptionThrowingAspect(expectedException), "someBean"));
		assertThat(advisors).as("One advice method was found").hasSize(1);
		ITestBean itb = createProxy(target, ITestBean.class, advisors);
		assertThatExceptionOfType(UndeclaredThrowableException.class)
			.isThrownBy(itb::getAge)
			.withCause(expectedException);
	}

	@Test
	void twoAdvicesOnOneAspect() {
		TestBean target = new TestBean();
		TwoAdviceAspect twoAdviceAspect = new TwoAdviceAspect();
		List<Advisor> advisors = getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(twoAdviceAspect, "someBean"));
		assertThat(advisors).as("Two advice methods found").hasSize(2);
		ITestBean itb = createProxy(target, ITestBean.class, advisors);
		itb.setName("");
		assertThat(itb.getAge()).isEqualTo(0);
		int newAge = 32;
		itb.setAge(newAge);
		assertThat(itb.getAge()).isEqualTo(1);
	}

	@Test
	void afterAdviceTypes() throws Exception {
		InvocationTrackingAspect aspect = new InvocationTrackingAspect();
		List<Advisor> advisors = getAdvisorFactory().getAdvisors(
			aspectInstanceFactory(aspect, "exceptionHandlingAspect"));
		Echo echo = createProxy(new Echo(), Echo.class, advisors);

		assertThat(aspect.invocations).isEmpty();
		assertThat(echo.echo(42)).isEqualTo(42);
		assertThat(aspect.invocations).containsExactly("around - start", "before", "after returning", "after", "around - end");

		aspect.invocations.clear();
		assertThatExceptionOfType(FileNotFoundException.class)
			.isThrownBy(() -> echo.echo(new FileNotFoundException()));
		assertThat(aspect.invocations).containsExactly("around - start", "before", "after throwing", "after", "around - end");
	}

	@Test
	void nonAbstractParentAspect() throws Exception {
		IncrementingAspect aspect = new IncrementingAspect();

		// Precondition:
		assertThat(Modifier.isAbstract(aspect.getClass().getSuperclass().getModifiers())).isFalse();

		List<Advisor> advisors = getAdvisorFactory().getAdvisors(aspectInstanceFactory(aspect, "incrementingAspect"));

		ITestBean proxy = createProxy(new TestBean("Jane", 42), ITestBean.class, advisors);
		assertThat(proxy.getAge()).isEqualTo(86); // (42 + 1) * 2
	}

	@Test
	void failureWithoutExplicitDeclarePrecedence() {
		TestBean target = new TestBean();
		MetadataAwareAspectInstanceFactory aspectInstanceFactory = aspectInstanceFactory(
				new NoDeclarePrecedenceShouldFail(), "someBean");
		ITestBean itb = createProxy(target, ITestBean.class, getAdvisorFactory().getAdvisors(aspectInstanceFactory));
		assertThat(itb.getAge()).isEqualTo(42);
	}

	@Test
	void declarePrecedenceNotSupported() {
		TestBean target = new TestBean();
		assertThatIllegalArgumentException().isThrownBy(() -> {
				MetadataAwareAspectInstanceFactory aspectInstanceFactory = aspectInstanceFactory(
							new DeclarePrecedenceShouldSucceed(), "someBean");
				createProxy(target, ITestBean.class, getAdvisorFactory().getAdvisors(aspectInstanceFactory));
		});
	}


	private static MetadataAwareAspectInstanceFactory aspectInstanceFactory(Object aspectInstance, String aspectName) {
		return new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName);
	}

	@SuppressWarnings("unchecked")
	private static <T> T createProxy(Object target, Class<T> type, List<Advisor> advisors) {
		ProxyFactory pf = new ProxyFactory(target);
		if (type.isInterface()) {
			pf.addInterface(type);
		}
		else {
			pf.setProxyTargetClass(true);
		}

		// Required everywhere we use AspectJ proxies
		pf.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		pf.addAdvisors(advisors);

		pf.setExposeProxy(true);
		return (T) pf.getProxy();
	}


	@Aspect("percflow(execution(* *(..)))")
	static class PerCflowAspect {
	}


	@Aspect("percflowbelow(execution(* *(..)))")
	static class PerCflowBelowAspect {
	}


	@Aspect("pertarget(execution(* *.getSpouse()))")
	@Order(10)
	static class PerTargetAspectWithOrderAnnotation10 {

		int count;

		@Around("execution(int *.getAge())")
		int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.set*(int))")
		void countSetter() {
			++count;
		}
	}


	@Aspect("pertarget(execution(* *.getSpouse()))")
	@Order(5)
	static class PerTargetAspectWithOrderAnnotation5 {

		int count;

		@Around("execution(int *.getAge())")
		int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.set*(int))")
		void countSetter() {
			++count;
		}
	}


	@Aspect("pertypewithin(org.springframework.beans.testfixture.beans.IOther+)")
	static class PerTypeWithinAspect {

		int count;

		@Around("execution(int *.getAge())")
		int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.*(..))")
		void countAnythingVoid() {
			++count;
		}
	}


	private class PerTypeWithinAspectInstanceFactory implements MetadataAwareAspectInstanceFactory {

		private int count;

		int getInstantiationCount() {
			return this.count;
		}

		@Override
		public Object getAspectInstance() {
			++this.count;
			return new PerTypeWithinAspect();
		}

		@Override
		public ClassLoader getAspectClassLoader() {
			return PerTypeWithinAspect.class.getClassLoader();
		}

		@Override
		public AspectMetadata getAspectMetadata() {
			return new AspectMetadata(PerTypeWithinAspect.class, "perTypeWithin");
		}

		@Override
		public Object getAspectCreationMutex() {
			return this;
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}
	}


	static class CommonPointcuts {

		@Pointcut("execution(* getAge())")
		void getAge() {
		}
	}

	@Aspect
	static class NamedPointcutAspectWithFQN {

		@SuppressWarnings("unused")
		private ITestBean fieldThatShouldBeIgnoredBySpringAtAspectJProcessing = new TestBean();

		@Around("org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.CommonPointcuts.getAge()()")
		int changeReturnValue(ProceedingJoinPoint pjp) {
			return -1;
		}
	}


	@Aspect
	static class NamedPointcutAspectWithoutFQN {

		@Pointcut("execution(* getAge())")
		void getAge() {
		}

		@Around("getAge()")
		int changeReturnValue(ProceedingJoinPoint pjp) {
			return -1;
		}
	}


	@Aspect
	static class NamedPointcutAspectFromLibrary {

		@Around("org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.Library.propertyAccess()")
		int changeReturnType(ProceedingJoinPoint pjp) {
			return -1;
		}

		@Around(value="org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.Library.integerArgOperation(x)", argNames="x")
		void doubleArg(ProceedingJoinPoint pjp, int x) throws Throwable {
			pjp.proceed(new Object[] {x*2});
		}
	}


	@Aspect
	static class Library {

		@Pointcut("execution(!void get*())")
		void propertyAccess() {}

		@Pointcut("execution(* *(..)) && args(i)")
		void integerArgOperation(int i) {}
	}


	@Aspect
	static class NamedPointcutAspectFromLibraryWithBinding {

		@Around(value="org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.Library.integerArgOperation(x)", argNames="x")
		void doubleArg(ProceedingJoinPoint pjp, int x) throws Throwable {
			pjp.proceed(new Object[] {x*2});
		}
	}


	@Aspect
	static class BindingAspectWithSingleArg {

		@Pointcut(value="args(a)", argNames="a")
		void setAge(int a) {}

		@Around(value="setAge(age)",argNames="age")
		// @ArgNames({"age"})	// AMC needs more work here? ignoring pjp arg... ok??
		// argNames should be supported in Around as it is in Pointcut
		void changeReturnType(ProceedingJoinPoint pjp, int age) throws Throwable {
			pjp.proceed(new Object[] {age*2});
		}
	}


	@Aspect
	static class ManyValuedArgs {

		String mungeArgs(String a, int b, int c, String d, StringBuilder e) {
			return a + b + c + d + e;
		}

		@Around(value="execution(String mungeArgs(..)) && args(a, b, c, d, e)", argNames="b,c,d,e,a")
		String reverseAdvice(ProceedingJoinPoint pjp, int b, int c, String d, StringBuilder e, String a) throws Throwable {
			assertThat(pjp.proceed()).isEqualTo(a + b+ c+ d+ e);
			return a + b + c + d + e;
		}
	}


	@Aspect
	static class ExceptionThrowingAspect {

		private final Exception ex;

		ExceptionThrowingAspect(Exception ex) {
			this.ex = ex;
		}

		@Before("execution(* getAge())")
		void throwException() throws Exception {
			throw ex;
		}
	}


	static class Echo {

		Object echo(Object obj) throws Exception {
			if (obj instanceof Exception ex) {
				throw ex;
			}
			return obj;
		}
	}


	@Aspect
	class DoublingAspect {

		@Around("execution(* getAge())")
		public Object doubleAge(ProceedingJoinPoint pjp) throws Throwable {
			return ((int) pjp.proceed()) * 2;
		}
	}

	@Aspect
	class IncrementingAspect extends DoublingAspect {

		@Around("execution(* getAge())")
		public int incrementAge(ProceedingJoinPoint pjp) throws Throwable {
			return ((int) pjp.proceed()) + 1;
		}
	}



	@Aspect
	private static class InvocationTrackingAspect {

		List<String> invocations = new ArrayList<>();


		@Pointcut("execution(* echo(*))")
		void echo() {
		}

		@Around("echo()")
		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
			}
		}

		@Before("echo()")
		void before() {
			invocations.add("before");
		}

		@AfterReturning("echo()")
		void afterReturning() {
			invocations.add("after returning");
		}

		@AfterThrowing("echo()")
		void afterThrowing() {
			invocations.add("after throwing");
		}

		@After("echo()")
		void after() {
			invocations.add("after");
		}
	}


	@Aspect
	static class NoDeclarePrecedenceShouldFail {

		@Pointcut("execution(int *.getAge())")
		void getAge() {
		}

		@Before("getAge()")
		void blowUpButDoesntMatterBecauseAroundAdviceWontLetThisBeInvoked() {
			throw new IllegalStateException();
		}

		@Around("getAge()")
		int preventExecution(ProceedingJoinPoint pjp) {
			return 42;
		}
	}


	@Aspect
	@DeclarePrecedence("test..*")
	static class DeclarePrecedenceShouldSucceed {

		@Pointcut("execution(int *.getAge())")
		void getAge() {
		}

		@Before("getAge()")
		void blowUpButDoesntMatterBecauseAroundAdviceWontLetThisBeInvoked() {
			throw new IllegalStateException();
		}

		@Around("getAge()")
		int preventExecution(ProceedingJoinPoint pjp) {
			return 42;
		}
	}

}


/**
 * Add a DeclareParents field in concrete subclasses, to identify
 * the type pattern to apply the introduction to.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@Aspect
abstract class AbstractMakeModifiable {

	interface MutableModifiable extends Modifiable {

		void markDirty();
	}

	static class ModifiableImpl implements MutableModifiable {

		private boolean modified;

		@Override
		public void acceptChanges() {
			modified = false;
		}

		@Override
		public boolean isModified() {
			return modified;
		}

		@Override
		public void markDirty() {
			this.modified = true;
		}
	}

	@Before(value="execution(void set*(*)) && this(modifiable) && args(newValue)", argNames="modifiable,newValue")
	void recordModificationIfSetterArgumentDiffersFromOldValue(
			JoinPoint jp, MutableModifiable mixin, Object newValue) {

		/*
		 * We use the mixin to check and, if necessary, change,
		 * modification status. We need the JoinPoint to get the
		 * setter method. We use newValue for comparison.
		 * We try to invoke the getter if possible.
		 */

		if (mixin.isModified()) {
			// Already changed, don't need to change again
			//System.out.println("changed");
			return;
		}

		// Find the current raw value, by invoking the corresponding setter
		Method correspondingGetter = getGetterFromSetter(((MethodSignature) jp.getSignature()).getMethod());
		boolean modified = true;
		if (correspondingGetter != null) {
			try {
				Object oldValue = correspondingGetter.invoke(jp.getTarget());
				//System.out.println("Old value=" + oldValue + "; new=" + newValue);
				modified = !ObjectUtils.nullSafeEquals(oldValue, newValue);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				// Don't sweat on exceptions; assume value was modified
			}
		}
		else {
			//System.out.println("cannot get getter for " + jp);
		}
		if (modified) {
			mixin.markDirty();
		}
	}

	private Method getGetterFromSetter(Method setter) {
		String getterName = setter.getName().replaceFirst("set", "get");
		try {
			return setter.getDeclaringClass().getMethod(getterName);
		}
		catch (NoSuchMethodException ex) {
			// must be write-only
			return null;
		}
	}

}


/**
 * Adds a declare parents pointcut.
 * @author Rod Johnson
 * @since 2.0
 */
@Aspect
class MakeITestBeanModifiable extends AbstractMakeModifiable {

	@DeclareParents(value = "org.springframework.beans.testfixture.beans.ITestBean+",
			defaultImpl=ModifiableImpl.class)
	static MutableModifiable mixin;

}


/**
 * Adds a declare parents pointcut - spr5307
 * @author Andy Clement
 * @since 3.0
 */
@Aspect
class MakeAnnotatedTypeModifiable extends AbstractMakeModifiable {

	@DeclareParents(value = "(@org.springframework.aop.aspectj.annotation.Measured *)",
			defaultImpl = DefaultLockable.class)
	static Lockable mixin;

}


/**
 * Demonstrates introductions, AspectJ annotation style.
 */
@Aspect
class MakeLockable {

	@DeclareParents(value = "org.springframework..*", defaultImpl = DefaultLockable.class)
	static Lockable mixin;

	@Before(value="execution(void set*(*)) && this(mixin)", argNames="mixin")
	void checkNotLocked( Lockable mixin) {
		// Can also obtain the mixin (this) this way
		//Lockable mixin = (Lockable) jp.getThis();
		if (mixin.locked()) {
			throw new IllegalStateException();
		}
	}

}


class CannotBeUnlocked implements Lockable, Comparable<Object> {

	@Override
	public void lock() {
	}

	@Override
	public void unlock() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean locked() {
		return true;
	}

	@Override
	public int compareTo(Object arg0) {
		throw new UnsupportedOperationException();
	}

}


/**
 * Used as a mixin.
 *
 * @author Rod Johnson
 */
interface Modifiable {

	boolean isModified();

	void acceptChanges();

}


/**
 * Used as a target.
 * @author Andy Clement
 */
interface AnnotatedTarget {
}


@Measured
class AnnotatedTargetImpl implements AnnotatedTarget {
}


@Retention(RetentionPolicy.RUNTIME)
@interface Measured {}

class NotLockable {

	private int intValue;

	int getIntValue() {
		return intValue;
	}

	void setIntValue(int intValue) {
		this.intValue = intValue;
	}

}


@Aspect("perthis(execution(* *.getSpouse()))")
class PerThisAspect {

	int count;

	// Just to check that this doesn't cause problems with introduction processing
	@SuppressWarnings("unused")
	private ITestBean fieldThatShouldBeIgnoredBySpringAtAspectJProcessing = new TestBean();

	@Around("execution(int *.getAge())")
	int returnCountAsAge() {
		return count++;
	}

	@Before("execution(void *.set*(int))")
	void countSetter() {
		++count;
	}

}
