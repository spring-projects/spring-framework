/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.config;

import org.springframework.beans.factory.parsing.ProblemReporter;

/**
 * Interface to be implemented by objects that specify the configuration of a particular feature
 * of the Spring container e.g., component-scanning, JMX MBean exporting, AspectJ auto-proxying,
 * annotation-driven transaction management, and so on.
 *
 * <p>Many features of the Spring container can be configured using either XML or annotations.
 * As one example, Spring's <em>component scanning</em> feature may be configured using
 * either the {@code <context:component-scan>} XML element or (as of Spring 3.1) the
 * {@code @ComponentScan} annotation. These two options are equivalent to one another, and users may
 * choose between them as a matter of convention or preference. Fundamentally, both are declarative
 * mechanisms for <em>specifying</em> how the Spring container should be configured.  A {@code
 * FeatureSpecification} object, then, is a way of representing this configuration information independent
 * of its original source format be it XML, annotations, or otherwise.
 *
 * <p>A {@code FeatureSpecification} is responsible for {@linkplain #validate validating itself}.
 * For example, a component-scanning specification would check that at least one base package has
 * been specified, and otherwise register a {@code Problem} with a {@link ProblemReporter}. Taking
 * this approach as opposed to throwing exceptions allows for maximum tooling and error reporting
 * flexibility.
 *
 * <p>A {@link FeatureSpecificationExecutor} is used to carry out the instructions within a populated
 * {@code FeatureSpecification}; this is where the "real work" happens. In the case of component scanning
 * as above, it is within a {@code FeatureSpecificationExecutor} that a bean definition scanner is created,
 * configured and invoked against the base packages specified.
 *
 * <p>{@code FeatureSpecification} objects may be populated and executed by Spring XML namespace element
 * parsers on order to separate the concerns of XML parsing from Spring bean definition creation and
 * registration. This type of use is mostly a framework-internal matter. More interesting to end users is
 * the use of {@code FeatureSpecification} objects within {@code @FeatureConfiguration} classes and their
 * {@code @Feature} methods. This functionality is new in Spring 3.1 and is the logical evolution of Spring's
 * Java-based configuration support first released in Spring 3.0 (see {@code @Configuration} classes and
 * {@code @Bean} methods). The primary goal of {@code Feature}-related support is to round out this
 * Java-based support and allow users to configure all aspects of the Spring-container without requiring
 * the use of XML. See "design notes" below for an example of this kind of use.
 *
 * <h2>Notes on designing {@code FeatureSpecification} implementations</h2>
 *
 * <p>The public API of a {@code FeatureSpecification} should be designed for maximum ease of use
 * within {@code @Feature} methods. Traditional JavaBean-style getters and setters should be dropped
 * for a more fluent style that allows for method chaining. Consider the following example of a
 * {@code @Feature} method:
 *
 * <pre>
 * &#64;Feature
 * public TxAnnotationDriven tx(PlatformTransactionManager txManager) {
 *    return new TxAnnotationDriven(txManager).proxyTargetClass(true);
 * }
 * </pre>
 *
 * Notice how the creation and configuration of the {@code TxAnnotationDriven} specification is
 * concise and reads well. This is facilitated by mutator methods that always return the
 * specification object's 'this' reference, allowing for method chaining. A secondary design goal
 * of this approach is that the resulting code tends to mirror corresponding XML namespace
 * declarations, which most Spring users are already familiar with. For example, compare the
 * code above with its XML counterpart:
 *
 * <p>{@code <tx:annotation-driven transaction-manager="txManager" proxy-target-class="true"/>}
 *
 * <p>Typically, a user will call only the constructor and 'mutator' methods of a specification
 * object. The accessor/getter methods, however, are typically called only by the specification's
 * {@linkplain FeatureSpecificationExecutor executor} for the purpose of populating and registering
 * bean definitions with the Spring container.For this reason, it is recommended that accessor
 * methods be given package-private visibility. This creates a better experience for users from
 * an IDE content-assist point of view as they will see only the public mutator methods, reducing
 * any possible confusion.
 *
 * <p>Implementations should take care to allow for use of string-based bean names, placeholder
 * (<code>"${...}"</code>) and SpEL (<code>"#{...}</code>) expressions wherever they may be useful.
 * While it is generally desirable to refer to dependent beans in pure Java, in certain cases a
 * user may wish or need to refer by bean name. For example, the {@code TxAnnotationDriven} specification
 * referenced above allows for specifying its transaction-manager reference by {@code String} or by
 * {@code PlatformTransactionManager} reference. Such strings should always be candidates for placeholder
 * replacement and SpEL evaluation for maximum configurability as well as parity with the featureset
 * available in Spring XML. With regard to SpEL expressions, users should assume that only expressions
 * evaluating to a bean name will be supported. While it is technically possible with SpEL to resolve
 * a bean instance, specification executors will not support such use unless explicitly indicated.
 *
 * <p>See the Javadoc for {@code @FeatureConfiguration} classes and {@code @Feature} methods for
 * information on their lifecycle and semantics.
 *
 * @author Chris Beams
 * @since 3.1
 * @see FeatureSpecificationExecutor
 * @see AbstractSpecificationExecutor
 * @see org.springframework.context.annotation.Feature
 * @see org.springframework.context.annotation.FeatureConfiguration
 */
public interface FeatureSpecification {

	/**
	 * Validate this specification instance to ensure all required properties
	 * have been set, including checks on mutually exclusive or mutually
	 * dependent properties. May in some cases modify the state of the
	 * specification e.g., instantiating types specified as strings.
	 * @see AbstractSpecificationExecutor#execute(Specification)
	 * @return whether any problems occurred during validation
	 */
	boolean validate(ProblemReporter problemReporter);

	/**
	 * Execute this specification instance, carrying out the instructions
	 * specified within. Should work by delegating to an underlying
	 * {@link FeatureSpecificationExecutor} for proper separation of concerns.
	 */
	void execute(ExecutorContext executorContext);

}
