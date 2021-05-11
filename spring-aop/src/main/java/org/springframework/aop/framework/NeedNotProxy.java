/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.framework;

/**
 * The final class can not proxy by cglib. So return the target object.
 *
 * @author Eric Cao
 * @since 05.11.2021
 */
public class NeedNotProxy implements AopProxy{

	Object target;

	public NeedNotProxy(Object target){
		this.target = target;
	}

	@Override
	public Object getProxy() {
		return this.target;
	}

	@Override
	public Object getProxy(ClassLoader classLoader) {
		return this.target;
	}
}
