/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Indexed;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Indexed
public interface FooService {

	String foo(int id);

	@Async
	Future<String> asyncFoo(int id);

	boolean isInitCalled();

}
