/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.tests.sample.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Simple model class to represent a collection of {@link TestBean}
 *
 * @author Tiago de Freitas Lima
 * @since 4.2.1
 */
public class TestBeans implements Iterable<TestBean> {

	private Collection<TestBean> beans;

	public TestBeans(Collection<TestBean> beans) {
		this.beans = new ArrayList<TestBean>(beans);
	}

	@Override
	public Iterator<TestBean> iterator() {
		return beans.iterator();
	}

}
