/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.aspectj;

/**
 * Generic-based dependency injection aspect.
 *
 * <p>This aspect allows users to implement efficient, type-safe dependency injection
 * without the use of the {@code @Configurable} annotation.
 *
 * <p>The subaspect of this aspect doesn't need to include any AOP constructs. For
 * example, here is a subaspect that configures the {@code PricingStrategyClient} objects.
 *
 * <pre class="code">
 * aspect PricingStrategyDependencyInjectionAspect
 *        extends GenericInterfaceDrivenDependencyInjectionAspect<PricingStrategyClient> {
 *     private PricingStrategy pricingStrategy;
 *
 *     public void configure(PricingStrategyClient bean) {
 *         bean.setPricingStrategy(pricingStrategy);
 *     }
 *
 *     public void setPricingStrategy(PricingStrategy pricingStrategy) {
 *         this.pricingStrategy = pricingStrategy;
 *     }
 * }</pre>
 *
 * @author Ramnivas Laddad
 * @since 3.0
 */
public abstract aspect GenericInterfaceDrivenDependencyInjectionAspect<I> extends AbstractInterfaceDrivenDependencyInjectionAspect {

	declare parents: I implements ConfigurableObject;

	public pointcut inConfigurableBean() : within(I+);

	@SuppressWarnings("unchecked")
	public final void configureBean(Object bean) {
		configure((I) bean);
	}

	// Unfortunately, erasure used with generics won't allow to use the same named method
	protected abstract void configure(I bean);

}
