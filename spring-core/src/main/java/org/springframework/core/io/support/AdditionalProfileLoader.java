package org.springframework.core.io.support;

/**
 * Load additional profiles using {@link SpringFactoriesLoader}
 * <p>
 * Example:
 * In this special scenario, where you have a spring-boot-starter with its own application.properties
 * with configured properties for e.g. multiple stages, you can implement this interface, so spring will include
 * the defined profile in its active profiles.
 *
 * If you want to order your profiles (like you would do with {@code spring.profiles.active}) you can annotate your
 * class with {@link org.springframework.core.annotation.Order}. Implementations without annotation will get implicitly
 * the lowest precedence by {@link SpringFactoriesLoader}.
 *
 * Example implementation:
 * <pre class="code">
 * class AdditionalCommonProfileLoader implements AdditionalProfileLoader {
 *
 *     &#064;Override
 *     public String useAdditionalProfile() {
 *         return "commons";
 *     }
 * }
 * </pre>
 * <p>
 * Add the following line to the spring-boot-starters {@code spring.factories}:
 * <pre class="code">org.springframework.core.io.support.AdditionalProfileLoader=example.AdditionalCommonProfileLoader</pre>
 *
 * Spring would now load an additional file "application-commons.yaml".
 *
 * @author Kevin Raddatz
 *
 * @see org.springframework.core.annotation.Order
 * @see SpringFactoriesLoader
 */
public interface AdditionalProfileLoader {

	String useAdditionalProfile();
}
