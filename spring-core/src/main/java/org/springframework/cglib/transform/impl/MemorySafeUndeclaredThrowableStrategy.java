/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cglib.transform.impl;

import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.core.TypeUtils;
import org.springframework.cglib.transform.ClassTransformer;
import org.springframework.cglib.transform.MethodFilter;
import org.springframework.cglib.transform.MethodFilterTransformer;
import org.springframework.cglib.transform.TransformingClassGenerator;

/**
 * Memory-safe variant of {@link UndeclaredThrowableStrategy} ported from the latest
 * as yet unreleased CGLIB code.
 *
 * @author Phillip Webb
 * @since 3.2.4
 */
public class MemorySafeUndeclaredThrowableStrategy extends DefaultGeneratorStrategy {

	private static final MethodFilter TRANSFORM_FILTER = new MethodFilter() {
		public boolean accept(int access, String name, String desc, String signature, String[] exceptions) {
			return !TypeUtils.isPrivate(access) && name.indexOf('$') < 0;
		}
	};


	private Class<?> wrapper;


	public MemorySafeUndeclaredThrowableStrategy(Class wrapper) {
		this.wrapper = wrapper;
	}


	protected ClassGenerator transform(ClassGenerator cg) throws Exception {
		ClassTransformer tr = new UndeclaredThrowableTransformer(wrapper);
		tr = new MethodFilterTransformer(TRANSFORM_FILTER, tr);
		return new TransformingClassGenerator(cg, tr);
	}

}
