/**
 *
 * Theme support classes for Spring's web MVC framework.
 * Provides standard ThemeResolver implementations,
 * and a HandlerInterceptor for theme changes.
 *
 * <p>
 * <ul>
 *   <li>If you don't provide a bean of one of these classes as {@code themeResolver},
 * a {@code FixedThemeResolver} will be provided with the default theme name 'theme'.</li>
 *   <li>If you use a defined {@code FixedThemeResolver}, you will able to use another theme
 * name for default, but the users will stick on this theme.</li>
 *   <li>With a {@code CookieThemeResolver} or {@code SessionThemeResolver}, you can allow
 * the user to change his current theme.</li>
 *   <li>Generally, you will put in the themes resource bundles the paths of CSS files, images and HTML constructs.</li>
 *   <li>For retrieving themes data, you can either use the spring:theme tag in JSP or access via the
 * {@code RequestContext} for other view technologies.</li>
 *   <li>The {@code pagedlist} demo application uses themes</li>
 * </ul>
 *
 */
package org.springframework.web.servlet.theme;

