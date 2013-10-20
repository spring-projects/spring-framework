/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.support;

import java.lang.reflect.Method;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.config.DefaultMvcUrlsFactoryBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A contract for creating URLs by referencing Spring MVC controllers and methods.
 * <p>
 * The MVC Java config and the MVC namespace automatically create an instance of this
 * contract for use in controllers and anywhere else during the processing of a request.
 * The best way for access it is to have it autowired, or otherwise injected either by
 * type or also qualified by name ("mvcUrls") if necessary.
 * <p>
 * If not using either option, with explicit configuration it's easy to create an instance
 * of {@link DefaultMvcUrls} in Java config or in XML configuration, use
 * {@link DefaultMvcUrlsFactoryBean}.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 *
 * @since 4.0
 */
public interface MvcUrls {

	/**
	 * Creates a new {@link UriComponentsBuilder} by pointing to a controller class. The
	 * resulting builder contains all the current request information up to and including
	 * the Servlet mapping as well as the portion of the path matching to the controller
	 * level request mapping. If the controller contains multiple mappings, the
	 * {@link DefaultMvcUrls} will use the first one.
	 *
	 * @param controllerType the controller type to create a URL to
	 *
	 * @return a builder that can be used to further build the {@link UriComponents}.
	 */
	UriComponentsBuilder linkToController(Class<?> controllerType);

	/**
	 * Create a {@link UriComponents} by pointing to a controller method along with method
	 * argument values.
	 * <p>
	 * Type and method-level mappings of the controller method are extracted and the
	 * resulting {@link UriComponents} is further enriched with method argument values from
	 * {@link PathVariable} and {@link RequestParam} parameters. Any other arguments not
	 * relevant to the building of the URL can be provided as {@literal null} and will be
	 * ignored. Support for additional custom arguments can be added through a
	 * {@link UriComponentsContributor}.
	 *
	 * FIXME Type-level URI template variables?
	 *
	 * @param method the target controller method
	 * @param argumentValues argument values matching to method parameters
	 *
	 * @return UriComponents instance, never {@literal null}
	 */
	UriComponents linkToMethod(Method method, Object... argumentValues);

	/**
	 * Create a {@link UriComponents} by invoking a method on a "mock" controller similar
	 * to how test frameworks provide mock objects and record method invocations. The
	 * static method {@link MvcUrlUtils#controller(Class, Object...)} can be used to
	 * create a "mock" controller:
	 *
	 * <pre class="code">
	 * &#064;RequestMapping("/people/{id}/addresses")
	 * class AddressController {
	 *
	 *   &#064;RequestMapping("/{country}")
	 *   public HttpEntity<Void> getAddressesForCountry(&#064;PathVariable String country) { … }
	 *
	 *   &#064;RequestMapping(value="/", method=RequestMethod.POST)
	 *   public void addAddress(Address address) { … }
	 * }
	 *
	 * // short-hand style with static import of MvcUrlUtils.controller
	 *
	 * mvcUrls.linkToMethodOn(controller(CustomerController.class, 1).showAddresses("US"));
	 *
	 * // longer style, required for void controller methods
	 *
	 * CustomerController controller = MvcUrlUtils.controller(CustomController.class, 1);
	 * controller.addAddress(null);
	 *
	 * mvcUrls.linkToMethodOn(controller);
	 *
	 * </pre>
	 *
	 * The above mechanism supports {@link PathVariable} and {@link RequestParam} method
	 * arguments. Any other arguments can be provided as {@literal null} and will be
	 * ignored. Additional custom arguments can be added through an implementation of
	 * {@link UriComponentsContributor}.
	 *
	 * @param mockController created via {@link MvcUrlUtils#controller(Class, Object...)}
	 *
	 * @return UriComponents instance, never {@literal null}
	 */
	UriComponents linkToMethodOn(Object mockController);

}
