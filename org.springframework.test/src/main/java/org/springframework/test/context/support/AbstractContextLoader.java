/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract application context loader, which provides a basis for all concrete
 * implementations of the {@link ContextLoader} strategy. Provides a
 * <em>Template Method</em> based approach for {@link #processLocations processing}
 * locations.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see #generateDefaultLocations
 * @see #modifyLocations
 */
public abstract class AbstractContextLoader implements ContextLoader {

	/**
	 * If the supplied <code>locations</code> are <code>null</code> or
	 * <em>empty</em> and {@link #isGenerateDefaultLocations()} is
	 * <code>true</code>, default locations will be
	 * {@link #generateDefaultLocations(Class) generated} for the specified
	 * {@link Class class} and the configured
	 * {@link #getResourceSuffix() resource suffix}; otherwise, the supplied
	 * <code>locations</code> will be {@link #modifyLocations modified} if
	 * necessary and returned.
	 * @param clazz the class with which the locations are associated: to be
	 * used when generating default locations
	 * @param locations the unmodified locations to use for loading the
	 * application context (can be <code>null</code> or empty)
	 * @return an array of application context resource locations
	 * @see #generateDefaultLocations
	 * @see #modifyLocations
	 * @see org.springframework.test.context.ContextLoader#processLocations
	 */
	public final String[] processLocations(Class<?> clazz, String... locations) {
		return (ObjectUtils.isEmpty(locations) && isGenerateDefaultLocations()) ?
				generateDefaultLocations(clazz) : modifyLocations(clazz, locations);
	}

	/**
	 * Generates the default classpath resource locations array based on the
	 * supplied class.
	 * <p>For example, if the supplied class is <code>com.example.MyTest</code>,
	 * the generated locations will contain a single string with a value of
	 * &quot;classpath:/com/example/MyTest<code>&lt;suffix&gt;</code>&quot;,
	 * where <code>&lt;suffix&gt;</code> is the value of the
	 * {@link #getResourceSuffix() resource suffix} string.
	 * <p>Subclasses can override this method to implement a different
	 * <em>default location generation</em> strategy.
	 * @param clazz the class for which the default locations are to be generated
	 * @return an array of default application context resource locations
	 * @see #getResourceSuffix()
	 */
	protected String[] generateDefaultLocations(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String suffix = getResourceSuffix();
		Assert.hasText(suffix, "Resource suffix must not be empty");
		return new String[] { ResourceUtils.CLASSPATH_URL_PREFIX + "/" +
				ClassUtils.convertClassNameToResourcePath(clazz.getName()) + suffix };
	}

	/**
	 * Generate a modified version of the supplied locations array and returns it.
	 * <p>A plain path, e.g. &quot;context.xml&quot;, will be treated as a
	 * classpath resource from the same package in which the specified class is
	 * defined. A path starting with a slash is treated as a fully qualified
	 * class path location, e.g.:
	 * &quot;/org/springframework/whatever/foo.xml&quot;. A path which
	 * references a URL (e.g., a path prefixed with
	 * {@link ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link ResourceUtils#FILE_URL_PREFIX file:}, <code>http:</code>,
	 * etc.) will be added to the results unchanged.
	 * <p>Subclasses can override this method to implement a different
	 * <em>location modification</em> strategy.
	 * @param clazz the class with which the locations are associated
	 * @param locations the resource locations to be modified
	 * @return an array of modified application context resource locations
	 */
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		String[] modifiedLocations = new String[locations.length];
		for (int i = 0; i < locations.length; i++) {
			String path = locations[i];
			if (path.startsWith("/")) {
				modifiedLocations[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			else if (!ResourcePatternUtils.isUrl(path)) {
				modifiedLocations[i] = ResourceUtils.CLASSPATH_URL_PREFIX + "/"
						+ StringUtils.cleanPath(ClassUtils.classPackageAsResourcePath(clazz) + "/" + path);
			}
			else {
				modifiedLocations[i] = StringUtils.cleanPath(path);
			}
		}
		return modifiedLocations;
	}


	/**
	 * Determine whether or not <em>default</em> resource locations should be
	 * generated if the <code>locations</code> provided to
	 * {@link #processLocations(Class,String...) processLocations()} are
	 * <code>null</code> or empty.
	 * <p>Can be overridden by subclasses to change the default behavior.
	 * @return always <code>true</code> by default
	 */
	protected boolean isGenerateDefaultLocations() {
		return true;
	}

	/**
	 * Get the suffix to append to {@link ApplicationContext} resource
	 * locations when generating default locations.
	 * <p>Must be implemented by subclasses.
	 * @return the resource suffix; should not be <code>null</code> or empty
	 * @see #generateDefaultLocations(Class)
	 */
	protected abstract String getResourceSuffix();

}
