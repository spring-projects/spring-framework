/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Lazy
public class AutowiredQualifierFooService implements FooService {

	@Autowired
	@Qualifier("testing")
	private FooDao fooDao;

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

	@Override
	public Future<String> asyncFoo(int id) {
		return CompletableFuture.completedFuture(this.fooDao.findFoo(id));
	}

	@Override
	public boolean isInitCalled() {
		return this.initCalled;
	}

}
