/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.support;

/**
 * Internal exception to be propagated from {@link ConstructorResolver},
 * passed through to the initiating {@link DefaultSingletonBeanRegistry}
 * (without wrapping in a {@code BeanCreationException}).
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
class ImplicitlyAppearedSingletonException extends IllegalStateException {

	public ImplicitlyAppearedSingletonException() {
		super("About-to-be-created singleton instance implicitly appeared through the " +
				"creation of the factory bean that its bean definition points to");
	}

}
