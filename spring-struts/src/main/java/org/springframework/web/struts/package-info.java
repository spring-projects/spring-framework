/**
 * Support classes for integrating a Struts web tier with a Spring middle
 * tier which is typically hosted in a Spring root WebApplicationContext.
 *
 * <p>Supports easy access to the Spring root WebApplicationContext
 * from Struts Actions via the ActionSupport and DispatchActionSupport
 * classes. Actions have full access to Spring's WebApplicationContext
 * facilities in this case, and explicitly look up Spring-managed beans.
 *
 * <p>Also supports wiring Struts Actions as Spring-managed beans in
 * a ContextLoaderPlugIn context, passing middle tier references to them
 * via bean references, using the Action path as bean name. There are two
 * ways to make Struts delegate Action lookup to the ContextLoaderPlugIn:
 *
 * <ul>
 * <li>Use DelegationActionProxy as Action "type" in struts-config.
 * There's no further setup necessary; you can choose any RequestProcessor.
 * Each such proxy will automatically delegate to the corresponding
 * Spring-managed Action bean in the ContextLoaderPlugIn context.
 *
 * <li>Configure DelegatingRequestProcessor as "processorClass" in
 * struts-config, using the original Action "type" (possibly generated
 * by XDoclet) or no "type" at all. To also use Tiles, configure
 * DelegatingTilesRequestProcessor instead.
 * </ul>
 */
@Deprecated
package org.springframework.web.struts;
