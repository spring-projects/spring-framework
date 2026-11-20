/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.context.annotation.Proxyable;
import org.springframework.stereotype.Service;

/**
 * @author Juergen Hoeller
 */
@Service @Proxyable(interfaces = FooService.class)
public class OtherFooService implements FooService {

	@Override
	public String foo(int id) {
		return "" + id;
	}

	@Override
	public Future<String> asyncFoo(int id) {
		return CompletableFuture.completedFuture("" + id);
	}

	@Override
	public boolean isInitCalled() {
		return false;
	}

}
