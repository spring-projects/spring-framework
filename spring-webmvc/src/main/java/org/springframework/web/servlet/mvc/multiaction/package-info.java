
/**
 *
 * Package allowing MVC Controller implementations to handle requests
 * at <i>method</i> rather than <i>class</i> level. This is useful when
 * we want to avoid having many trivial controller classes, as can
 * easily happen when using an MVC framework.
 *
 * <p>Typically a controller that handles multiple request types will
 * extend MultiActionController, and implement multiple request handling
 * methods that will be invoked by reflection if they follow this class'
 * naming convention. Classes are analyzed at startup and methods cached,
 * so the performance overhead of reflection in this approach is negligible.
 *
 * <p>This approach is analogous to the Struts 1.1 <b>DispatcherAction</b>
 * class, but more sophisticated, as it supports configurable mapping from
 * requests to URLs and allows for delegation as well as subclassing.
 *
 * <p>This package is discussed in Chapter 12 of <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson, and used in the sample application.
 *
 */
package org.springframework.web.servlet.mvc.multiaction;

