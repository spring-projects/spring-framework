/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.context.annotation.Scope;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Scope("myScope")
public class ScopedProxyTestBean implements FooService {

	@Override
	public String foo(int id) {
		return "bar";
	}

	@Override
	public Future<String> asyncFoo(int id) {
		return CompletableFuture.completedFuture("bar");
	}

	@Override
	public boolean isInitCalled() {
		return false;
	}

}
