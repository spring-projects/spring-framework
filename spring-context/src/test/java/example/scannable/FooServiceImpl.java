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

package example.scannable;

import java.util.List;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.DependsOn;
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
public class FooServiceImpl implements FooService {

	@Autowired private FooDao fooDao;

	@Autowired public BeanFactory beanFactory;

	@Autowired public List<ListableBeanFactory> listableBeanFactory;

	@Autowired public ResourceLoader resourceLoader;

	@Autowired public ResourcePatternResolver resourcePatternResolver;

	@Autowired public ApplicationEventPublisher eventPublisher;

	@Autowired public MessageSource messageSource;

	@Autowired public ApplicationContext context;

	@Autowired public ConfigurableApplicationContext[] configurableContext;

	@Autowired public AbstractApplicationContext genericContext;

	private boolean initCalled = false;

	@SuppressWarnings("unused")
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

	@Override
	public Future<String> asyncFoo(int id) {
		System.out.println(Thread.currentThread().getName());
		Assert.state(ServiceInvocationCounter.getThreadLocalCount() != null, "Thread-local counter not exposed");
		return new AsyncResult<String>(this.fooDao.findFoo(id));
	}

	@Override
	public boolean isInitCalled() {
		return this.initCalled;
	}

}
