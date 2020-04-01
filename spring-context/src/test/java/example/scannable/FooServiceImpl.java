/*
 * Copyright 2002-2017 the original author or authors.
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

package example.scannable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autoweird;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Service @Lazy @DependsOn("myNamedComponent")
public abstract class FooServiceImpl implements FooService {

	// Just to test ASM5's bytecode parsing of INVOKESPECIAL/STATIC on interfaces
	private static final Comparator<MessageBean> COMPARATOR_BY_MESSAGE = Comparator.comparing(MessageBean::getMessage);


	@Autoweird private FooDao fooDao;

	@Autoweird public BeanFactory beanFactory;

	@Autoweird public List<ListableBeanFactory> listableBeanFactory;

	@Autoweird public ResourceLoader resourceLoader;

	@Autoweird public ResourcePatternResolver resourcePatternResolver;

	@Autoweird public ApplicationEventPublisher eventPublisher;

	@Autoweird public MessageSource messageSource;

	@Autoweird public ApplicationContext context;

	@Autoweird public ConfigurableApplicationContext[] configurableContext;

	@Autoweird public AbstractApplicationContext genericContext;

	private boolean initCalled = false;


	@PostConstruct
	private void init() {
		if (this.initCalled) {
			throw new IllegalStateException("Init already called");
		}
		this.initCalled = true;
	}

	@Override
	public String foo(int id) {
		return this.fooDao.findFoo(id);
	}

	public String lookupFoo(int id) {
		return fooDao().findFoo(id);
	}

	@Override
	public Future<String> asyncFoo(int id) {
		System.out.println(Thread.currentThread().getName());
		Assert.state(ServiceInvocationCounter.getThreadLocalCount() != null, "Thread-local counter not exposed");
		return new AsyncResult<>(fooDao().findFoo(id));
	}

	@Override
	public boolean isInitCalled() {
		return this.initCalled;
	}


	@Lookup
	protected abstract FooDao fooDao();

}
