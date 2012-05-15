/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Indicates that a method produces a bean to be managed by the Spring container. The
 * names and semantics of the attributes to this annotation are intentionally similar
 * to those of the {@code <bean/>} element in the Spring XML schema. For example:
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }</pre>
 *
 * <p>While a {@link #name()} attribute is available, the default strategy for determining
 * the name of a bean is to use the name of the Bean method. This is convenient and
 * intuitive, but if explicit naming is desired, the {@code name()} attribute may be used.
 * Also note that {@code name()} accepts an array of Strings. This is in order to allow
 * for specifying multiple names (i.e., aliases) for a single bean.
 * <pre class="code">
 *     &#064;Bean(name={"b1","b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }</pre>
 *
 * <p>Note that the {@code @Bean} annotation does not provide attributes for scope,
 * primary or lazy. Rather, it should be used in conjunction with {@link Scope @Scope},
 * {@link Primary @Primary}, and {@link Lazy @Lazy} annotations to achieve
 * those semantics. For example:
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }</pre>
 *
 * <p>Typically, {@code @Bean} methods are declared within {@code @Configuration}
 * classes. In this case, bean methods may reference other <code>@Bean</code> methods
 * on the same class by calling them <i>directly</i>. This ensures that references between
 * beans are strongly typed and navigable. Such so-called 'inter-bean references' are
 * guaranteed to respect scoping and AOP semantics, just like <code>getBean</code> lookups
 * would. These are the semantics known from the original 'Spring JavaConfig' project
 * which require CGLIB subclassing of each such configuration class at runtime. As a
 * consequence, {@code @Configuration} classes and their factory methods must not be
 * marked as final or private in this mode. For example:
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *     // ...
 * }</pre>
 *
 * <p>{@code @Bean} methods may also be declared within any {@code @Component} class, in
 * which case they will get processed in a configuration class <em>'lite'</em> mode in which
 * they will simply be called as plain factory methods from the container (similar to
 * {@code factory-method} declarations in XML). The containing component classes remain
 * unmodified in this case, and there are no unusual constraints for factory methods;
 * however, scoping semantics are <b>not</b> respected as described above for inter-bean method
 * invocations in this mode. For example:
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <p>See @{@link Configuration} Javadoc for further details including how to bootstrap
 * the container using {@link AnnotationConfigApplicationContext} and friends.
 *
 * <h3>A note on {@code BeanFactoryPostProcessor}-returning {@code @Bean} methods</h3>
 * <p>Special consideration must be taken for {@code @Bean} methods that return Spring
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * ({@code BFPP}) types. Because {@code BFPP} objects must be instantiated very early in the
 * container lifecycle, they can interfere with processing of annotations such as {@code @Autowired},
 * {@code @Value}, and {@code @PostConstruct} within {@code @Configuration} classes. To avoid these
 * lifecycle issues, mark {@code BFPP}-returning {@code @Bean} methods as {@code static}. For example:
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertyPlaceholderConfigurer ppc() {
 *         // instantiate, configure and return ppc ...
 *     }</pre>
 * By marking this method as {@code static}, it can be invoked without causing instantiation of its
 * declaring {@code @Configuration} class, thus avoiding the above-mentioned lifecycle conflicts.
 * Note however that {@code static} {@code @Bean} methods will not be enhanced for scoping and AOP
 * semantics as mentioned above. This works out in {@code BFPP} cases, as they are not typically
 * referenced by other {@code @Bean} methods. As a reminder, a WARN-level log message will be
 * issued for any non-static {@code @Bean} methods having a return type assignable to
 * {@code BeanFactoryPostProcessor}.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.stereotype.Component
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	/**
	 * The name of this bean, or if plural, aliases for this bean. If left unspecified
	 * the name of the bean is the name of the annotated method. If specified, the method
	 * name is ignored.
	 */
	String[] name() default {};

	/**
	 * Are dependencies to be injected via autowiring?
	 */
	Autowire autowire() default Autowire.NO;

	/**
	 * The optional name of a method to call on the bean instance during initialization.
	 * Not commonly used, given that the method may be called programmatically directly
	 * within the body of a Bean-annotated method. Default value is {@code ""}, indicating
	 * that no init method should be called.
	 */
	String initMethod() default "";

	/**
	 * The optional name of a method to call on the bean instance upon closing the
	 * application context, for example a {@code close()} method on a JDBC {@code
	 * DataSource} implementation, or a Hibernate {@code SessionFactory} object.
	 * The method must have no arguments but may throw any exception.
	 * <p>As a convenience to the user, the container will attempt to infer a destroy
	 * method against object returned from the {@code @Bean} method. For example, given a
	 * {@code @Bean} method returning an Apache Commons DBCP {@code BasicDataSource}, the
	 * container will notice the {@code close()} method available on that object and
	 * automatically register it as the {@code destroyMethod}. This 'destroy method
	 * inference' is currently limited to detecting only public, no-arg methods named
	 * 'close'. The method may be declared at any level of the inheritance hierarchy, and
	 * will be detected regardless of the return type of the {@code @Bean} method, i.e.
	 * detection occurs reflectively against the bean instance itself at creation time.
	 * <p>To disable destroy method inference for a particular {@code @Bean}, specify an
	 * empty string as the value, e.g. {@code @Bean(destroyMethod="")}.
	 * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
	 * factory, which is always the case for singletons but not guaranteed 
	 * for any other scope.
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
