package org.springframework.core.io.support;

/**
 * Load additional profiles using {@link SpringFactoriesLoader}
 * <p>
 * Example:
 * In this special scenario, where you have a spring-boot-starter with its own application.properties
 * with configured properties for e.g. multiple stages, you can implement this interface, so spring will include
 * the defined profile in its active profiles.
 *
 * Example implementation:
 * <pre class="code">
 * class AdditionalCommonProfileLoader implements AdditionalProfilesLoader {
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
 * @author Kevin Raddatz
 */
public interface AdditionalProfileLoader {

	String useAdditionalProfile();
}
