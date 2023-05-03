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

package org.springframework.transaction.annotation;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;
import org.springframework.transaction.testfixture.ReactiveCallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Paluch
 */
public class AnnotationTransactionInterceptorTests {

	private final CallCountingTransactionManager ptm = new CallCountingTransactionManager();

	private final ReactiveCallCountingTransactionManager rtm = new ReactiveCallCountingTransactionManager();

	private final AnnotationTransactionAttributeSource source = new AnnotationTransactionAttributeSource();

	private final TransactionInterceptor ti = new TransactionInterceptor((TransactionManager) this.ptm, this.source);


	@Test
	public void classLevelOnly() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestClassLevelOnly());
		proxyFactory.addAdvice(this.ti);
		TestClassLevelOnly proxy = (TestClassLevelOnly) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(4);
	}

	@Test
	public void withSingleMethodOverride() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithSingleMethodOverride());
		proxyFactory.addAdvice(this.ti);
		TestWithSingleMethodOverride proxy = (TestWithSingleMethodOverride) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomethingCompletelyElse();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(4);
	}

	@Test
	public void withSingleMethodOverrideInverted() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithSingleMethodOverrideInverted());
		proxyFactory.addAdvice(this.ti);
		TestWithSingleMethodOverrideInverted proxy = (TestWithSingleMethodOverrideInverted) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomethingCompletelyElse();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(4);
	}

	@Test
	public void withMultiMethodOverride() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithMultiMethodOverride());
		proxyFactory.addAdvice(this.ti);
		TestWithMultiMethodOverride proxy = (TestWithMultiMethodOverride) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomethingCompletelyElse();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(4);
	}

	@Test
	public void withRollbackOnRuntimeException() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithExceptions());
		proxyFactory.addAdvice(this.ti);
		TestWithExceptions proxy = (TestWithExceptions) proxyFactory.getProxy();

		assertThatIllegalStateException().isThrownBy(
				proxy::doSomethingErroneous)
			.satisfies(ex -> assertGetTransactionAndRollbackCount(1));

		assertThatIllegalArgumentException().isThrownBy(
				proxy::doSomethingElseErroneous)
			.satisfies(ex -> assertGetTransactionAndRollbackCount(2));
	}

	@Test
	public void withCommitOnCheckedException() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithExceptions());
		proxyFactory.addAdvice(this.ti);
		TestWithExceptions proxy = (TestWithExceptions) proxyFactory.getProxy();

		assertThatException()
			.isThrownBy(proxy::doSomethingElseWithCheckedException)
			.satisfies(ex -> assertGetTransactionAndCommitCount(1));
	}

	@Test
	public void withRollbackOnCheckedExceptionAndRollbackRule() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithExceptions());
		proxyFactory.addAdvice(this.ti);
		TestWithExceptions proxy = (TestWithExceptions) proxyFactory.getProxy();

		assertThatException()
			.isThrownBy(proxy::doSomethingElseWithCheckedExceptionAndRollbackRule)
			.satisfies(ex -> assertGetTransactionAndRollbackCount(1));
	}

	@Test
	public void withMonoSuccess() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithReactive());
		proxyFactory.addAdvice(new TransactionInterceptor(rtm, this.source));
		TestWithReactive proxy = (TestWithReactive) proxyFactory.getProxy();

		StepVerifier.withVirtualTime(proxy::monoSuccess).thenAwait(Duration.ofSeconds(10)).verifyComplete();
		assertReactiveGetTransactionAndCommitCount(1);
	}

	@Test
	public void withMonoFailure() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithReactive());
		proxyFactory.addAdvice(new TransactionInterceptor(rtm, this.source));
		TestWithReactive proxy = (TestWithReactive) proxyFactory.getProxy();

		proxy.monoFailure().as(StepVerifier::create).verifyError();
		assertReactiveGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withMonoRollback() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithReactive());
		proxyFactory.addAdvice(new TransactionInterceptor(rtm, this.source));
		TestWithReactive proxy = (TestWithReactive) proxyFactory.getProxy();

		StepVerifier.withVirtualTime(proxy::monoSuccess).thenAwait(Duration.ofSeconds(1)).thenCancel().verify();
		assertReactiveGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withFluxSuccess() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithReactive());
		proxyFactory.addAdvice(new TransactionInterceptor(rtm, this.source));
		TestWithReactive proxy = (TestWithReactive) proxyFactory.getProxy();

		StepVerifier.withVirtualTime(proxy::fluxSuccess).thenAwait(Duration.ofSeconds(10)).expectNextCount(1).verifyComplete();
		assertReactiveGetTransactionAndCommitCount(1);
	}

	@Test
	public void withFluxFailure() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithReactive());
		proxyFactory.addAdvice(new TransactionInterceptor(rtm, this.source));
		TestWithReactive proxy = (TestWithReactive) proxyFactory.getProxy();

		proxy.fluxFailure().as(StepVerifier::create).verifyError();
		assertReactiveGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withFluxRollback() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithReactive());
		proxyFactory.addAdvice(new TransactionInterceptor(rtm, this.source));
		TestWithReactive proxy = (TestWithReactive) proxyFactory.getProxy();

		StepVerifier.withVirtualTime(proxy::fluxSuccess).thenAwait(Duration.ofSeconds(1)).thenCancel().verify();
		assertReactiveGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withCompletableFutureSuccess() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithCompletableFuture());
		proxyFactory.addAdvice(this.ti);
		TestWithCompletableFuture proxy = (TestWithCompletableFuture) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);
	}

	@Test
	public void withCompletableFutureRuntimeException() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithCompletableFuture());
		proxyFactory.addAdvice(this.ti);
		TestWithCompletableFuture proxy = (TestWithCompletableFuture) proxyFactory.getProxy();

		proxy.doSomethingErroneous();
		assertGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withCompletableFutureCheckedException() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithCompletableFuture());
		proxyFactory.addAdvice(this.ti);
		TestWithCompletableFuture proxy = (TestWithCompletableFuture) proxyFactory.getProxy();

		proxy.doSomethingErroneousWithCheckedException();
		assertGetTransactionAndCommitCount(1);
	}

	@Test
	public void withCompletableFutureCheckedExceptionAndRollbackRule() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithCompletableFuture());
		proxyFactory.addAdvice(this.ti);
		TestWithCompletableFuture proxy = (TestWithCompletableFuture) proxyFactory.getProxy();

		proxy.doSomethingErroneousWithCheckedExceptionAndRollbackRule();
		assertGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withVavrTrySuccess() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithVavrTry());
		proxyFactory.addAdvice(this.ti);
		TestWithVavrTry proxy = (TestWithVavrTry) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);
	}

	@Test
	public void withVavrTryRuntimeException() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithVavrTry());
		proxyFactory.addAdvice(this.ti);
		TestWithVavrTry proxy = (TestWithVavrTry) proxyFactory.getProxy();

		proxy.doSomethingErroneous();
		assertGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withVavrTryCheckedException() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithVavrTry());
		proxyFactory.addAdvice(this.ti);
		TestWithVavrTry proxy = (TestWithVavrTry) proxyFactory.getProxy();

		proxy.doSomethingErroneousWithCheckedException();
		assertGetTransactionAndCommitCount(1);
	}

	@Test
	public void withVavrTryCheckedExceptionAndRollbackRule() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithVavrTry());
		proxyFactory.addAdvice(this.ti);
		TestWithVavrTry proxy = (TestWithVavrTry) proxyFactory.getProxy();

		proxy.doSomethingErroneousWithCheckedExceptionAndRollbackRule();
		assertGetTransactionAndRollbackCount(1);
	}

	@Test
	public void withInterface() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new TestWithInterfaceImpl());
		proxyFactory.addInterface(TestWithInterface.class);
		proxyFactory.addAdvice(this.ti);
		TestWithInterface proxy = (TestWithInterface) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(4);

		proxy.doSomethingDefault();
		assertGetTransactionAndCommitCount(5);
	}

	@Test
	public void crossClassInterfaceMethodLevelOnJdkProxy() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new SomeServiceImpl());
		proxyFactory.addInterface(SomeService.class);
		proxyFactory.addAdvice(this.ti);
		SomeService someService = (SomeService) proxyFactory.getProxy();

		someService.bar();
		assertGetTransactionAndCommitCount(1);

		someService.foo();
		assertGetTransactionAndCommitCount(2);

		someService.fooBar();
		assertGetTransactionAndCommitCount(3);
	}

	@Test
	public void crossClassInterfaceOnJdkProxy() {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(new OtherServiceImpl());
		proxyFactory.addInterface(OtherService.class);
		proxyFactory.addAdvice(this.ti);
		OtherService otherService = (OtherService) proxyFactory.getProxy();

		otherService.foo();
		assertGetTransactionAndCommitCount(1);
	}

	@Test
	public void withInterfaceOnTargetJdkProxy() {
		ProxyFactory targetFactory = new ProxyFactory();
		targetFactory.setTarget(new TestWithInterfaceImpl());
		targetFactory.addInterface(TestWithInterface.class);

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(targetFactory.getProxy());
		proxyFactory.addInterface(TestWithInterface.class);
		proxyFactory.addAdvice(this.ti);
		TestWithInterface proxy = (TestWithInterface) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(4);

		proxy.doSomethingDefault();
		assertGetTransactionAndCommitCount(5);
	}

	@Test
	public void withInterfaceOnTargetCglibProxy() {
		ProxyFactory targetFactory = new ProxyFactory();
		targetFactory.setTarget(new TestWithInterfaceImpl());
		targetFactory.setProxyTargetClass(true);

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(targetFactory.getProxy());
		proxyFactory.addInterface(TestWithInterface.class);
		proxyFactory.addAdvice(this.ti);
		TestWithInterface proxy = (TestWithInterface) proxyFactory.getProxy();

		proxy.doSomething();
		assertGetTransactionAndCommitCount(1);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(2);

		proxy.doSomethingElse();
		assertGetTransactionAndCommitCount(3);

		proxy.doSomething();
		assertGetTransactionAndCommitCount(4);

		proxy.doSomethingDefault();
		assertGetTransactionAndCommitCount(5);
	}

	private void assertGetTransactionAndCommitCount(int expectedCount) {
		assertThat(this.ptm.begun).isEqualTo(expectedCount);
		assertThat(this.ptm.commits).isEqualTo(expectedCount);
	}

	private void assertGetTransactionAndRollbackCount(int expectedCount) {
		assertThat(this.ptm.begun).isEqualTo(expectedCount);
		assertThat(this.ptm.rollbacks).isEqualTo(expectedCount);
	}

	private void assertReactiveGetTransactionAndCommitCount(int expectedCount) {
		assertThat(this.rtm.begun).isEqualTo(expectedCount);
		assertThat(this.rtm.commits).isEqualTo(expectedCount);
	}

	private void assertReactiveGetTransactionAndRollbackCount(int expectedCount) {
		assertThat(this.rtm.begun).isEqualTo(expectedCount);
		assertThat(this.rtm.rollbacks).isEqualTo(expectedCount);
	}


	@Transactional
	public static class TestClassLevelOnly {

		public void doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}

		public void doSomethingElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}
	}


	@Transactional
	public static class TestWithSingleMethodOverride {

		public void doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}

		@Transactional(readOnly = true)
		public void doSomethingElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
		}

		public void doSomethingCompletelyElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}
	}


	@Transactional(readOnly = true)
	public static class TestWithSingleMethodOverrideInverted {

		@Transactional
		public void doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}

		public void doSomethingElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
		}

		public void doSomethingCompletelyElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
		}
	}


	@Transactional
	public static class TestWithMultiMethodOverride {

		@Transactional(readOnly = true)
		public void doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
		}

		@Transactional(readOnly = true)
		public void doSomethingElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
		}

		public void doSomethingCompletelyElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}
	}


	@Transactional
	public static class TestWithExceptions {

		public void doSomethingErroneous() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			throw new IllegalStateException();
		}

		public void doSomethingElseErroneous() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			throw new IllegalArgumentException();
		}

		@Transactional
		public void doSomethingElseWithCheckedException() throws Exception {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			throw new Exception();
		}

		@Transactional(rollbackFor = Exception.class)
		public void doSomethingElseWithCheckedExceptionAndRollbackRule() throws Exception {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			throw new Exception();
		}
	}


	@Transactional
	public static class TestWithReactive {

		public Mono<Void> monoSuccess() {
			return Mono.delay(Duration.ofSeconds(10)).then();
		}

		public Mono<Void> monoFailure() {
			return Mono.error(new IllegalStateException());
		}

		public Flux<Object> fluxSuccess() {
			return Flux.just(new Object()).delayElements(Duration.ofSeconds(10));
		}

		public Flux<Object> fluxFailure() {
			return Flux.error(new IllegalStateException());
		}
	}


	@Transactional
	public static class TestWithCompletableFuture {

		public CompletableFuture<String> doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return CompletableFuture.completedFuture("ok");
		}

		public CompletableFuture<String> doSomethingErroneous() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return CompletableFuture.failedFuture(new IllegalStateException());
		}

		public CompletableFuture<String> doSomethingErroneousWithCheckedException() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return CompletableFuture.failedFuture(new Exception());
		}

		@Transactional(rollbackFor = Exception.class)
		public CompletableFuture<String> doSomethingErroneousWithCheckedExceptionAndRollbackRule() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return CompletableFuture.failedFuture(new Exception());
		}
	}


	@Transactional
	public static class TestWithVavrTry {

		public Try<String> doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return Try.success("ok");
		}

		public Try<String> doSomethingErroneous() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return Try.failure(new IllegalStateException());
		}

		public Try<String> doSomethingErroneousWithCheckedException() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return Try.failure(new Exception());
		}

		@Transactional(rollbackFor = Exception.class)
		public Try<String> doSomethingErroneousWithCheckedExceptionAndRollbackRule() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			return Try.failure(new Exception());
		}
	}


	public interface BaseInterface {

		void doSomething();
	}


	@Transactional
	public interface TestWithInterface extends BaseInterface {

		@Transactional(readOnly = true)
		void doSomethingElse();

		default void doSomethingDefault() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}
	}


	public static class TestWithInterfaceImpl implements TestWithInterface {

		@Override
		public void doSomething() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		}

		@Override
		public void doSomethingElse() {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
		}
	}


	public interface SomeService {

		void foo();

		@Transactional
		void bar();

		@Transactional(readOnly = true)
		void fooBar();
	}


	public static class SomeServiceImpl implements SomeService {

		@Override
		public void bar() {
		}

		@Override
		@Transactional
		public void foo() {
		}

		@Override
		@Transactional(readOnly = false)
		public void fooBar() {
		}
	}


	public interface OtherService {

		void foo();
	}


	@Transactional
	public static class OtherServiceImpl implements OtherService {

		@Override
		public void foo() {
		}
	}

}
