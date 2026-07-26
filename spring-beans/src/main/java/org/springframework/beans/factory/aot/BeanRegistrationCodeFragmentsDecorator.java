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

package org.springframework.beans.factory.aot;

import java.util.function.UnaryOperator;

import guru.mocker.annotation.mixin.Mixin;

import org.springframework.util.Assert;

/**
 * A {@link BeanRegistrationCodeFragments} decorator implementation. Typically
 * used when part of the default code fragments have to customized, by extending
 * this class and using it as part of
 * {@link BeanRegistrationAotContribution#withCustomCodeFragments(UnaryOperator)}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
@Mixin
public class BeanRegistrationCodeFragmentsDecorator extends BeanRegistrationCodeFragmentsForwarder implements BeanRegistrationCodeFragments {

	protected BeanRegistrationCodeFragmentsDecorator(BeanRegistrationCodeFragments delegate) {
		super(delegate);
		Assert.notNull(delegate, "Delegate must not be null");
	}

}
