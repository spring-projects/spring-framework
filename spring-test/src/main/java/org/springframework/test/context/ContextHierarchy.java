/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ContextHierarchy} is an annotation that can be applied to a test class
 * to define a hierarchy of {@link org.springframework.context.ApplicationContext
 * ApplicationContexts} for integration tests.
 *
 * <h3>Examples</h3>
 *
 * <p>The following JUnit-based examples demonstrate common configuration
 * scenarios for integration tests that require the use of context hierarchies.
 *
 * <h4>Single Test Class with Context Hierarchy</h4>
 *
 * <p>{@code ControllerIntegrationTests} represents a typical integration testing
 * scenario for a Spring MVC web application by declaring a context hierarchy
 * consisting of two levels, one for the <em>root</em> {@code WebApplicationContext}
 * (with {@code TestAppConfig}) and one for the <em>dispatcher servlet</em>
 * {@code WebApplicationContext} (with {@code WebConfig}). The {@code
 * WebApplicationContext} that is <em>autowired</em> into the test instance is
 * the one for the child context (i.e., the lowest context in the hierarchy).
 *
 * <pre class="code">
 * &#064;ExtendWith(SpringExtension.class)
 * &#064;WebAppConfiguration
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(classes = TestAppConfig.class),
 *     &#064;ContextConfiguration(classes = WebConfig.class)
 * })
 * class ControllerIntegrationTests {
 *
 *     &#064;Autowired
 *     WebApplicationContext wac;
 *
 *     // ...
 * }</pre>
 *
 * <h4>Class Hierarchy with Implicit Parent Context</h4>
 *
 * <p>The following test classes define a context hierarchy within a test class
 * hierarchy. {@code AbstractWebTests} declares the configuration for a root
 * {@code WebApplicationContext} in a Spring-powered web application. Note,
 * however, that {@code AbstractWebTests} does not declare {@code @ContextHierarchy};
 * consequently, subclasses of {@code AbstractWebTests} can optionally participate
 * in a context hierarchy or follow the standard semantics for {@code @ContextConfiguration}.
 * {@code SoapWebServiceTests} and {@code RestWebServiceTests} both extend
 * {@code AbstractWebTests} and define a context hierarchy via {@code @ContextHierarchy}.
 * The result is that three application contexts will be loaded (one for each
 * declaration of {@code @ContextConfiguration}), and the application context
 * loaded based on the configuration in {@code AbstractWebTests} will be set as
 * the parent context for each of the contexts loaded for the concrete subclasses.
 *
 * <pre class="code">
 * &#064;ExtendWith(SpringExtension.class)
 * &#064;WebAppConfiguration
 * &#064;ContextConfiguration("file:src/main/webapp/WEB-INF/applicationContext.xml")
 * public abstract class AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/spring/soap-ws-config.xml"))
 * public class SoapWebServiceTests extends AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/spring/rest-ws-config.xml"))
 * public class RestWebServiceTests extends AbstractWebTests {}</pre>
 *
 * <h4>Class Hierarchy with Merged Context Hierarchy Configuration</h4>
 *
 * <p>The following classes demonstrate the use of <em>named</em> hierarchy levels
 * in order to <em>merge</em> the configuration for specific levels in a context
 * hierarchy. {@code BaseTests} defines two levels in the hierarchy, {@code "parent"}
 * and {@code "child"}. {@code ExtendedTests} extends {@code BaseTests} and instructs
 * the Spring TestContext Framework to merge the context configuration for the
 * {@code "child"} hierarchy level, simply by ensuring that the names declared via
 * {@link ContextConfiguration#name} are both {@code "child"}. The result is that
 * three application contexts will be loaded: one for {@code "/app-config.xml"},
 * one for {@code "/user-config.xml"}, and one for <code>{"/user-config.xml",
 * "/order-config.xml"}</code>. As with the previous example, the application
 * context loaded from {@code "/app-config.xml"} will be set as the parent context
 * for the contexts loaded from {@code "/user-config.xml"} and <code>{"/user-config.xml",
 * "/order-config.xml"}</code>.
 *
 * <pre class="code">
 * &#064;ExtendWith(SpringExtension.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 *     &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 *     &#064;ContextConfiguration(name = "child",  locations = "/order-config.xml")
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <h4>Class Hierarchy with Overridden Context Hierarchy Configuration</h4>
 *
 * <p>In contrast to the previous example, this example demonstrates how to
 * <em>override</em> the configuration for a given named level in a context hierarchy
 * by setting the {@link ContextConfiguration#inheritLocations} flag to {@code false}.
 * Consequently, the application context for {@code ExtendedTests} will be loaded
 * only from {@code "/test-user-config.xml"} and will have its parent set to the
 * context loaded from {@code "/app-config.xml"}.
 *
 * <pre class="code">
 * &#064;ExtendWith(SpringExtension.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 *     &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 *     &#064;ContextConfiguration(name = "child",  locations = "/test-user-config.xml", inheritLocations = false)
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <h4>Context Hierarchies with Bean Overrides</h4>
 *
 * <p>When {@code @ContextHierarchy} is used in conjunction with bean overrides such as
 * {@link org.springframework.test.context.bean.override.convention.TestBean @TestBean},
 * {@link org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean}, or
 * {@link org.springframework.test.context.bean.override.mockito.MockitoSpyBean @MockitoSpyBean},
 * it may be desirable or necessary to have the override applied to a single level
 * in the context hierarchy. To achieve that, the bean override must specify a
 * context name that matches a name configured via {@link ContextConfiguration#name}.
 *
 * <p>The following test class configures the name of the second hierarchy level to be
 * {@code "user-config"} and simultaneously specifies that the {@code UserService} should
 * be wrapped in a Mockito spy in the context named {@code "user-config"}. Consequently,
 * Spring will only attempt to create the spy in the {@code "user-config"} context and will
 * not attempt to create the spy in the parent context.
 *
 * <pre class="code">
 * &#064;ExtendWith(SpringExtension.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(classes = AppConfig.class),
 *     &#064;ContextConfiguration(classes = UserConfig.class, name = "user-config")
 * })
 * class IntegrationTests {
 *
 *     &#064;MockitoSpyBean(contextName = "user-config")
 *     UserService userService;
 *
 *     // ...
 * }</pre>
 *
 * <p>When applying bean overrides in different levels of the context hierarchy, you may
 * need to have all of the bean override instances injected into the test class in order
 * to interact with them &mdash; for example, to configure stubbing for mocks. However,
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} will always
 * inject a matching bean found in the lowest level of the context hierarchy. Thus, to
 * inject bean override instances from specific levels in the context hierarchy, you need
 * to annotate fields with appropriate bean override annotations and configure the name
 * of the context level.
 *
 * <p>The following test class configures the names of the hierarchy levels to be
 * {@code "parent"} and {@code "child"}. It also declares two {@code PropertyService}
 * fields that are configured to create or replace {@code PropertyService} beans with
 * Mockito mocks in the respective contexts, named {@code "parent"} and {@code "child"}.
 * Consequently, the mock from the {@code "parent"} context will be injected into the
 * {@code propertyServiceInParent} field, and the mock from the {@code "child"} context
 * will be injected into the {@code propertyServiceInChild} field.
 *
 * <pre class="code">
 * &#064;ExtendWith(SpringExtension.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(classes = ParentConfig.class, name = "parent"),
 *     &#064;ContextConfiguration(classes = ChildConfig.class, name = "child")
 * })
 * class IntegrationTests {
 *
 *     &#064;MockitoBean(contextName = "parent")
 *     PropertyService propertyServiceInParent;
 *
 *     &#064;MockitoBean(contextName = "child")
 *     PropertyService propertyServiceInChild;
 *
 *     // ...
 * }</pre>
 *
 * <h4>Miscellaneous</h4>
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>This annotation will be inherited from an enclosing test class by default.
 * See {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @since 3.2.2
 * @see ContextConfiguration
 * @see org.springframework.context.ApplicationContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextHierarchy {

	/**
	 * A list of {@link ContextConfiguration @ContextConfiguration} instances,
	 * each of which defines a level in the context hierarchy.
	 * <p>If you need to merge or override the configuration for a given level
	 * of the context hierarchy within a test class hierarchy, you must explicitly
	 * name that level by supplying the same value to the {@link ContextConfiguration#name
	 * name} attribute in {@code @ContextConfiguration} at each level in the
	 * class hierarchy. See the class-level Javadoc for examples.
	 */
	ContextConfiguration[] value();

}
