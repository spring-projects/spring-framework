
/**
 *
 * <p>
 *     Standard controller implementations for the portlet MVC framework that
 *     comes with Spring. Provides both abstract base classes and concrete
 *     implementations for often seen use cases.
 * </p>
 * 
 * <p>
 *     A <code>Controller</code> - as defined in this package - is analogous to a Struts
 *     <code>Action</code>. Usually <code>Controllers</code> are JavaBeans
 *     to allow easy configuration using the {@link org.springframework.beans org.springframework.beans}
 *     package. Controllers define the <code>C</code> from so-called MVC paradigm
 *     and can be used in conjunction with the {@link org.springframework.web.portlet.ModelAndView ModelAndView}
 *     to achieve interactive applications. The view might be represented by a
 *     HTML interface, but, because of model and the controller being completely
 *     independent of the view, PDF views are possible, as well as for instance Excel
 *     views.
 * </p>
 * 
 * <p>
 *     Especially useful to read, while getting into the Spring MVC framework
 *     are the following:
 *     <ul>
 *         <li><a href="Controller.html">Controller</a></li>
 *         <li><a href="SimpleFormController.html">BaseCommandController</a></li>
 *         <li><a href="ParameterizableViewController.html">ParameterizableViewController</a></li>
 *     </ul>
 * </p>
 *
 */
package org.springframework.web.portlet.mvc;

