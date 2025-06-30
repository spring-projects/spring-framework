/*
 * Copyright 2002-present the original author or authors.
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
