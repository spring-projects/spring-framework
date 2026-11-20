/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Service @Primary @Lazy @DependsOn("myNamedComponent")
public abstract class FooServiceImpl implements FooService {

	// Just to test ASM5's bytecode parsing of INVOKESPECIAL/STATIC on interfaces
	@SuppressWarnings("unused")
	private static final Comparator<MessageBean> COMPARATOR_BY_MESSAGE = Comparator.comparing(MessageBean::getMessage);


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
		Assert.state(ServiceInvocationCounter.getThreadLocalCount() != null, "Thread-local counter not exposed");
		return CompletableFuture.completedFuture(fooDao().findFoo(id));
	}

	@Override
	public boolean isInitCalled() {
		return this.initCalled;
	}


	@Lookup
	protected abstract FooDao fooDao();

}
