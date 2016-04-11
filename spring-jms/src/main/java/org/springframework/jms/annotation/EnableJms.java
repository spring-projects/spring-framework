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

package org.springframework.jms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Enable JMS listener annotated endpoints that are created under the cover
 * by a {@link org.springframework.jms.config.JmsListenerContainerFactory
 * JmsListenerContainerFactory}. To be used on
 * {@link org.springframework.context.annotation.Configuration Configuration}
 * classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public DefaultJmsListenerContainerFactory myJmsListenerContainerFactory() {
 *       DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
 *       factory.setConnectionFactory(connectionFactory());
 *       factory.setDestinationResolver(destinationResolver());
 *       factory.setConcurrency("5");
 *       return factory;
 *     }
 *
 *     // other &#064;Bean definitions
 * }</pre>
 *
 * The {@code JmsListenerContainerFactory} is responsible to create the listener container
 * responsible for a particular endpoint. Typical implementations, as the
 * {@link org.springframework.jms.config.DefaultJmsListenerContainerFactory DefaultJmsListenerContainerFactory}
 * used in the sample above, provides the necessary configuration options that are supported by
 * the underlying {@link org.springframework.jms.listener.MessageListenerContainer MessageListenerContainer}.
 *
 * <p>{@code @EnableJms} enables detection of {@link JmsListener} annotations on any
 * Spring-managed bean in the container. For example, given a class {@code MyService}:
 *
 * <pre class="code">
 * package com.acme.foo;
 *
 * public class MyService {
 *
 *     &#064;JmsListener(containerFactory = "myJmsListenerContainerFactory", destination="myQueue")
 *     public void process(String msg) {
 *         // process incoming message
 *     }
 * }</pre>
 *
 * The container factory to use is identified by the {@link JmsListener#containerFactory() containerFactory}
 * attribute defining the name of the {@code JmsListenerContainerFactory} bean to use.  When none
 * is set a {@code JmsListenerContainerFactory} bean with name {@code jmsListenerContainerFactory} is
 * assumed to be present.
 *
 * <p>the following configuration would ensure that every time a {@link javax.jms.Message}
 * is received on the {@link javax.jms.Destination} named "myQueue", {@code MyService.process()}
 * is called with the content of the message:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 *     // JMS infrastructure setup
 * }</pre>
 *
 * Alternatively, if {@code MyService} were annotated with {@code @Component}, the
 * following configuration would ensure that its {@code @JmsListener} annotated
 * method is invoked with a matching incoming message:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * &#064;ComponentScan(basePackages="com.acme.foo")
 * public class AppConfig {
 * }</pre>
 *
 * Note that the created containers are not registered against the application context
 * but can be easily located for management purposes using the
 * {@link org.springframework.jms.config.JmsListenerEndpointRegistry JmsListenerEndpointRegistry}.
 *
 * <p>Annotated methods can use flexible signature; in particular, it is possible to use
 * the {@link org.springframework.messaging.Message Message} abstraction and related annotations,
 * see {@link JmsListener} Javadoc for more details. For instance, the following would
 * inject the content of the message and a a custom "myCounter" JMS header:
 *
 * <pre class="code">
 * &#064;JmsListener(containerFactory = "myJmsListenerContainerFactory", destination="myQueue")
 * public void process(String msg, @Header("myCounter") int counter) {
 *     // process incoming message
 * }</pre>
 *
 * These features are abstracted by the {@link org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory}
 * that is responsible to build the necessary invoker to process the annotated method. By default,
 * {@link org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory} is used.
 *
 * <p>When more control is desired, a {@code @Configuration} class may implement
 * {@link JmsListenerConfigurer}. This allows access to the underlying
 * {@link org.springframework.jms.config.JmsListenerEndpointRegistrar JmsListenerEndpointRegistrar}
 * instance. The following example demonstrates how to specify an explicit default
 * {@code JmsListenerContainerFactory}
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         registrar.setContainerFactory(myJmsListenerContainerFactory());
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerContainerFactory<?> myJmsListenerContainerFactory() {
 *         // factory settings
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * {@code <beans>
 *
 *     <jms:annotation-driven container-factory="myJmsListenerContainerFactory"/>
 *
 *     <bean id="myJmsListenerContainerFactory" class="org.springframework.jms.config.DefaultJmsListenerContainerFactory">
 *           // factory settings
 *     </bean>
 *
 *     <bean id="myService" class="com.acme.foo.MyService"/>
 *
 * </beans>
 * }</pre>
 *
 * It is also possible to specify a custom {@link org.springframework.jms.config.JmsListenerEndpointRegistry
 * JmsListenerEndpointRegistry} in case you need more control on the way the containers
 * are created and managed. The example below also demonstrates how to customize the
 * {@code JmsHandlerMethodFactory} to use with a custom {@link org.springframework.validation.Validator
 * Validator} so that payloads annotated with {@link org.springframework.validation.annotation.Validated
 * Validated} are first validated against a custom {@code Validator}.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         registrar.setEndpointRegistry(myJmsListenerEndpointRegistry());
 *         registrar.setMessageHandlerMethodFactory(myJmsHandlerMethodFactory);
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerEndpointRegistry<?> myJmsListenerEndpointRegistry() {
 *         // registry configuration
 *     }
 *
 *     &#064;Bean
 *     public JmsHandlerMethodFactory myJmsHandlerMethodFactory() {
 *        DefaultJmsHandlerMethodFactory factory = new DefaultJmsHandlerMethodFactory();
 *        factory.setValidator(new MyValidator());
 *        return factory;
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * For reference, the example above can be compared to the following Spring XML
 * configuration:
 * <pre class="code">
 * {@code <beans>
 *
 *     <jms:annotation-driven registry="myJmsListenerEndpointRegistry"
 *         handler-method-factory="myJmsHandlerMethodFactory"/&gt;
 *
 *     <bean id="myJmsListenerEndpointRegistry"
 *           class="org.springframework.jms.config.JmsListenerEndpointRegistry">
 *           // registry configuration
 *     </bean>
 *
 *     <bean id="myJmsHandlerMethodFactory"
 *           class="org.springframework.messaging.handler.support.DefaultJmsHandlerMethodFactory">
 *         <property name="validator" ref="myValidator"/>
 *     </bean>
 *
 *     <bean id="myService" class="com.acme.foo.MyService"/>
 *
 * </beans>
 * }</pre>
 *
 * Implementing {@code JmsListenerConfigurer} also allows for fine-grained
 * control over endpoints registration via the {@code JmsListenerEndpointRegistrar}.
 * For example, the following configures an extra endpoint:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         SimpleJmsListenerEndpoint myEndpoint = new SimpleJmsListenerEndpoint();
 *         // ... configure the endpoint
 *         registrar.registerEndpoint(endpoint, anotherJmsListenerContainerFactory());
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerContainerFactory<?> anotherJmsListenerContainerFactory() {
 *         // ...
 *     }
 *
 *     // JMS infrastructure setup
 * }</pre>
 *
 * Note that all beans implementing {@code JmsListenerConfigurer} will be detected and
 * invoked in a similar fashion. The example above can be translated in a regular bean
 * definition registered in the context in case you use the XML configuration.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JmsListener
 * @see JmsListenerAnnotationBeanPostProcessor
 * @see org.springframework.jms.config.JmsListenerEndpointRegistrar
 * @see org.springframework.jms.config.JmsListenerEndpointRegistry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(JmsBootstrapConfiguration.class)
public @interface EnableJms {
}
