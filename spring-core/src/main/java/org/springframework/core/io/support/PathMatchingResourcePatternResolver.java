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

package org.springframework.core.io.support;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.VfsResource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourcePatternResolver} implementation that is able to resolve a
 * specified resource location path into one or more matching Resources.
 * The source path may be a simple path which has a one-to-one mapping to a
 * target {@link org.springframework.core.io.Resource}, or alternatively
 * may contain the special "{@code classpath*:}" prefix and/or
 * internal Ant-style regular expressions (matched using Spring's
 * {@link org.springframework.util.AntPathMatcher} utility).
 * Both of the latter are effectively wildcards.
 *
 * <p><b>No Wildcards:</b>
 *
 * <p>In the simple case, if the specified location path does not start with the
 * {@code "classpath*:}" prefix, and does not contain a PathMatcher pattern,
 * this resolver will simply return a single resource via a
 * {@code getResource()} call on the underlying {@code ResourceLoader}.
 * Examples are real URLs such as "{@code file:C:/context.xml}", pseudo-URLs
 * such as "{@code classpath:/context.xml}", and simple unprefixed paths
 * such as "{@code /WEB-INF/context.xml}". The latter will resolve in a
 * fashion specific to the underlying {@code ResourceLoader} (e.g.
 * {@code ServletContextResource} for a {@code WebApplicationContext}).
 *
 * <p><b>Ant-style Patterns:</b>
 *
 * <p>When the path location contains an Ant-style pattern, e.g.:
 * <pre class="code">
 * /WEB-INF/*-context.xml
 * com/mycompany/**&#47;applicationContext.xml
 * file:C:/some/path/*-context.xml
 * classpath:com/mycompany/**&#47;applicationContext.xml</pre>
 * the resolver follows a more complex but defined procedure to try to resolve
 * the wildcard. It produces a {@code Resource} for the path up to the last
 * non-wildcard segment and obtains a {@code URL} from it. If this URL is
 * not a "{@code jar:}" URL or container-specific variant (e.g.
 * "{@code zip:}" in WebLogic, "{@code wsjar}" in WebSphere", etc.),
 * then a {@code java.io.File} is obtained from it, and used to resolve the
 * wildcard by walking the filesystem. In the case of a jar URL, the resolver
 * either gets a {@code java.net.JarURLConnection} from it, or manually parses
 * the jar URL, and then traverses the contents of the jar file, to resolve the
 * wildcards.
 *
 * <p><b>Implications on portability:</b>
 *
 * <p>If the specified path is already a file URL (either explicitly, or
 * implicitly because the base {@code ResourceLoader} is a filesystem one,
 * then wildcarding is guaranteed to work in a completely portable fashion.
 *
 * <p>If the specified path is a classpath location, then the resolver must
 * obtain the last non-wildcard path segment URL via a
 * {@code Classloader.getResource()} call. Since this is just a
 * node of the path (not the file at the end) it is actually undefined
 * (in the ClassLoader Javadocs) exactly what sort of a URL is returned in
 * this case. In practice, it is usually a {@code java.io.File} representing
 * the directory, where the classpath resource resolves to a filesystem
 * location, or a jar URL of some sort, where the classpath resource resolves
 * to a jar location. Still, there is a portability concern on this operation.
 *
 * <p>If a jar URL is obtained for the last non-wildcard segment, the resolver
 * must be able to get a {@code java.net.JarURLConnection} from it, or
 * manually parse the jar URL, to be able to walk the contents of the jar,
 * and resolve the wildcard. This will work in most environments, but will
 * fail in others, and it is strongly recommended that the wildcard
 * resolution of resources coming from jars be thoroughly tested in your
 * specific environment before you rely on it.
 *
 * <p><b>{@code classpath*:} Prefix:</b>
 *
 * <p>There is special support for retrieving multiple class path resources with
 * the same name, via the "{@code classpath*:}" prefix. For example,
 * "{@code classpath*:META-INF/beans.xml}" will find all "beans.xml"
 * files in the class path, be it in "classes" directories or in JAR files.
 * This is particularly useful for autodetecting config files of the same name
 * at the same location within each jar file. Internally, this happens via a
 * {@code ClassLoader.getResources()} call, and is completely portable.
 *
 * <p>The "classpath*:" prefix can also be combined with a PathMatcher pattern in
 * the rest of the location path, for example "classpath*:META-INF/*-beans.xml".
 * In this case, the resolution strategy is fairly simple: a
 * {@code ClassLoader.getResources()} call is used on the last non-wildcard
 * path segment to get all the matching resources in the class loader hierarchy,
 * and then off each resource the same PathMatcher resolution strategy described
 * above is used for the wildcard subpath.
 *
 * <p><b>Other notes:</b>
 *
 * <p><b>WARNING:</b> Note that "{@code classpath*:}" when combined with
 * Ant-style patterns will only work reliably with at least one root directory
 * before the pattern starts, unless the actual target files reside in the file
 * system. This means that a pattern like "{@code classpath*:*.xml}" will
 * <i>not</i> retrieve files from the root of jar files but rather only from the
 * root of expanded directories. This originates from a limitation in the JDK's
 * {@code ClassLoader.getResources()} method which only returns file system
 * locations for a passed-in empty String (indicating potential roots to search).
 * This {@code ResourcePatternResolver} implementation is trying to mitigate the
 * jar root lookup limitation through {@link URLClassLoader} introspection and
 * "java.class.path" manifest evaluation; however, without portability guarantees.
 *
 * <p><b>WARNING:</b> Ant-style patterns with "classpath:" resources are not
 * guaranteed to find matching resources if the root package to search is available
 * in multiple class path locations. This is because a resource such as
 * <pre class="code">
 *     com/mycompany/package1/service-context.xml
 * </pre>
 * may be in only one location, but when a path such as
 * <pre class="code">
 *     classpath:com/mycompany/**&#47;service-context.xml
 * </pre>
 * is used to try to resolve it, the resolver will work off the (first) URL
 * returned by {@code getResource("com/mycompany");}. If this base package node
 * exists in multiple classloader locations, the actual end resource may not be
 * underneath. Therefore, preferably, use "{@code classpath*:}" with the same
 * Ant-style pattern in such a case, which will search <i>all</i> class path
 * locations that contain the root package.
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Marius Bogoevici
 * @author Costin Leau
 * @author Phil Webb
 * @since 1.0.2
 * @see #CLASSPATH_ALL_URL_PREFIX
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.core.io.ResourceLoader#getResource(String)
 * @see ClassLoader#getResources(String)
 */
public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);

	private static Method equinoxResolveMethod;

	static {
		try {
			// Detect Equinox OSGi (e.g. on WebSphere 6.1)
			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
					PathMatchingResourcePatternResolver.class.getClassLoader());
			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
			logger.debug("Found Equinox FileLocator for OSGi bundle URL resolution");
		}
		catch (Throwable ex) {
			equinoxResolveMethod = null;
		}
	}


	private final ResourceLoader resourceLoader;

	private PathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
	 * <p>ClassLoader access will happen via the thread context class loader.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * Create a new PathMatchingResourcePatternResolver.
	 * <p>ClassLoader access will happen via the thread context class loader.
	 * @param resourceLoader the ResourceLoader to load root directories and
	 * actual resources with
	 */
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
	 * @param classLoader the ClassLoader to load classpath resources with,
	 * or {@code null} for using the thread context class loader
	 * at the time of actual resource access
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
	}


	/**
	 * Return the ResourceLoader that this pattern resolver works with.
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	public ClassLoader getClassLoader() {
		return getResourceLoader().getClassLoader();
	}

	/**
	 * Set the PathMatcher implementation to use for this
	 * resource pattern resolver. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the PathMatcher that this resource pattern resolver uses.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	@Override
	public Resource getResource(String location) {
		return getResourceLoader().getResource(location);
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			// a class path resource (multiple resources for same name possible)
			if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
				// a class path resource pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// all class path resources with the given name
				return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
			}
		}
		else {
			// Only look for a pattern after a prefix here
			// (to not get fooled by a pattern symbol in a strange prefix).
			int prefixEnd = locationPattern.indexOf(":") + 1;
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				// a file pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// a single resource with the given name
				return new Resource[] {getResourceLoader().getResource(locationPattern)};
			}
		}
	}

	/**
	 * Find all class location resources with the given location via the ClassLoader.
	 * Delegates to {@link #doFindAllClassPathResources(String)}.
	 * @param location the absolute path within the classpath
	 * @return the result as Resource array
	 * @throws IOException in case of I/O errors
	 * @see java.lang.ClassLoader#getResources
	 * @see #convertClassLoaderURL
	 */
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = location;
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		Set<Resource> result = doFindAllClassPathResources(path);
		if (logger.isDebugEnabled()) {
			logger.debug("Resolved classpath location [" + location + "] to resources " + result);
		}
		return result.toArray(new Resource[result.size()]);
	}

	/**
	 * Find all class location resources with the given path via the ClassLoader.
	 * Called by {@link #findAllClassPathResources(String)}.
	 * @param path the absolute path within the classpath (never a leading slash)
	 * @return a mutable Set of matching Resource instances
	 * @since 4.1.1
	 */
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(16);
		ClassLoader cl = getClassLoader();
		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
		while (resourceUrls.hasMoreElements()) {
			URL url = resourceUrls.nextElement();
			result.add(convertClassLoaderURL(url));
		}
		if ("".equals(path)) {
			// The above result is likely to be incomplete, i.e. only containing file system references.
			// We need to have pointers to each of the jar files on the classpath as well...
			addAllClassLoaderJarRoots(cl, result);
		}
		return result;
	}

	/**
	 * Convert the given URL as returned from the ClassLoader into a {@link Resource}.
	 * <p>The default implementation simply creates a {@link UrlResource} instance.
	 * @param url a URL as returned from the ClassLoader
	 * @return the corresponding Resource object
	 * @see java.lang.ClassLoader#getResources
	 * @see org.springframework.core.io.Resource
	 */
	protected Resource convertClassLoaderURL(URL url) {
		return new UrlResource(url);
	}

	/**
	 * Search all {@link URLClassLoader} URLs for jar file references and add them to the
	 * given set of resources in the form of pointers to the root of the jar file content.
	 * @param classLoader the ClassLoader to search (including its ancestors)
	 * @param result the set of resources to add jar roots to
	 * @since 4.1.1
	 */
	protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
		if (classLoader instanceof URLClassLoader) {
			try {
				for (URL url : ((URLClassLoader) classLoader).getURLs()) {
					try {
						UrlResource jarResource = new UrlResource(
								ResourceUtils.JAR_URL_PREFIX + url.toString() + ResourceUtils.JAR_URL_SEPARATOR);
						if (jarResource.exists()) {
							result.add(jarResource);
						}
					}
					catch (MalformedURLException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Cannot search for matching files underneath [" + url +
									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
						}
					}
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
							"] does not support 'getURLs()': " + ex);
				}
			}
		}

		if (classLoader == ClassLoader.getSystemClassLoader()) {
			// "java.class.path" manifest evaluation...
			addClassPathManifestEntries(result);
		}

		if (classLoader != null) {
			try {
				// Hierarchy traversal...
				addAllClassLoaderJarRoots(classLoader.getParent(), result);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
							"] does not support 'getParent()': " + ex);
				}
			}
		}
	}

	/**
	 * Determine jar file references from the "java.class.path." manifest property and add them
	 * to the given set of resources in the form of pointers to the root of the jar file content.
	 * @param result the set of resources to add jar roots to
	 * @since 4.3
	 */
	protected void addClassPathManifestEntries(Set<Resource> result) {
		try {
			String javaClassPathProperty = System.getProperty("java.class.path");
			for (String path : StringUtils.delimitedListToStringArray(
					javaClassPathProperty, System.getProperty("path.separator"))) {
				try {
					File file = new File(path);
					UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
							ResourceUtils.FILE_URL_PREFIX + file.getAbsolutePath() +
							ResourceUtils.JAR_URL_SEPARATOR);
					if (jarResource.exists()) {
						result.add(jarResource);
					}
				}
				catch (MalformedURLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Cannot search for matching files underneath [" + path +
								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
					}
				}
			}
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
			}
		}
	}

	/**
	 * Find all resources that match the given location pattern via the
	 * Ant-style PathMatcher. Supports resources in jar files and zip files
	 * and in the file system.
	 * @param locationPattern the location pattern to match
	 * @return the result as Resource array
	 * @throws IOException in case of I/O errors
	 * @see #doFindPathMatchingJarResources
	 * @see #doFindPathMatchingFileResources
	 * @see org.springframework.util.PathMatcher
	 */
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		String rootDirPath = determineRootDir(locationPattern);
		String subPattern = locationPattern.substring(rootDirPath.length());
		Resource[] rootDirResources = getResources(rootDirPath);
		Set<Resource> result = new LinkedHashSet<>(16);
		for (Resource rootDirResource : rootDirResources) {
			rootDirResource = resolveRootDirResource(rootDirResource);
			URL rootDirURL = rootDirResource.getURL();
			if (equinoxResolveMethod != null) {
				if (rootDirURL.getProtocol().startsWith("bundle")) {
					rootDirURL = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirURL);
					rootDirResource = new UrlResource(rootDirURL);
				}
			}
			if (rootDirURL.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirURL, subPattern, getPathMatcher()));
			}
			else if (ResourceUtils.isJarURL(rootDirURL) || isJarResource(rootDirResource)) {
				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirURL, subPattern));
			}
			else {
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		return result.toArray(new Resource[result.size()]);
	}

	/**
	 * Determine the root directory for the given location.
	 * <p>Used for determining the starting point for file matching,
	 * resolving the root directory location to a {@code java.io.File}
	 * and passing it into {@code retrieveMatchingFiles}, with the
	 * remainder of the location as pattern.
	 * <p>Will return "/WEB-INF/" for the pattern "/WEB-INF/*.xml",
	 * for example.
	 * @param location the location to check
	 * @return the part of the location that denotes the root directory
	 * @see #retrieveMatchingFiles
	 */
	protected String determineRootDir(String location) {
		int prefixEnd = location.indexOf(":") + 1;
		int rootDirEnd = location.length();
		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
		}
		if (rootDirEnd == 0) {
			rootDirEnd = prefixEnd;
		}
		return location.substring(0, rootDirEnd);
	}

	/**
	 * Resolve the specified resource for path matching.
	 * <p>By default, Equinox OSGi "bundleresource:" / "bundleentry:" URL will be
	 * resolved into a standard jar file URL that be traversed using Spring's
	 * standard jar file traversal algorithm. For any preceding custom resolution,
	 * override this method and replace the resource handle accordingly.
	 * @param original the resource to resolve
	 * @return the resolved resource (may be identical to the passed-in resource)
	 * @throws IOException in case of resolution failure
	 */
	protected Resource resolveRootDirResource(Resource original) throws IOException {
		return original;
	}

	/**
	 * Return whether the given resource handle indicates a jar resource
	 * that the {@code doFindPathMatchingJarResources} method can handle.
	 * <p>By default, the URL protocols "jar", "zip", "vfszip and "wsjar"
	 * will be treated as jar resources. This template method allows for
	 * detecting further kinds of jar-like resources, e.g. through
	 * {@code instanceof} checks on the resource handle type.
	 * @param resource the resource handle to check
	 * (usually the root directory to start path matching from)
	 * @see #doFindPathMatchingJarResources
	 * @see org.springframework.util.ResourceUtils#isJarURL
	 */
	protected boolean isJarResource(Resource resource) throws IOException {
		return false;
	}

	/**
	 * Find all resources in jar files that match the given location pattern
	 * via the Ant-style PathMatcher.
	 * @param rootDirResource the root directory as Resource
	 * @param rootDirURL the pre-resolved root directory URL
	 * @param subPattern the sub pattern to match (below the root directory)
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException in case of I/O errors
	 * @since 4.3
	 * @see java.net.JarURLConnection
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirURL, String subPattern)
			throws IOException {

		URLConnection con = rootDirURL.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean closeJarFile;

		if (con instanceof JarURLConnection) {
			// Should usually be the case for traditional JAR files.
			JarURLConnection jarCon = (JarURLConnection) con;
			ResourceUtils.useCachesIfNecessary(jarCon);
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
			closeJarFile = !jarCon.getUseCaches();
		}
		else {
			// No JarURLConnection -> need to resort to URL file parsing.
			// We'll assume URLs of the format "jar:path!/entry", with the protocol
			// being arbitrary as long as following the entry format.
			// We'll also handle paths with and without leading "file:" prefix.
			String urlFile = rootDirURL.getFile();
			try {
				int separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + ResourceUtils.JAR_URL_SEPARATOR.length());
					jarFile = getJarFile(jarFileUrl);
				}
				else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			}
			catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// Root entry path must end with slash to allow for proper matching.
				// The Sun JRE does not return a slash here, but BEA JRockit does.
				rootEntryPath = rootEntryPath + "/";
			}
			Set<Resource> result = new LinkedHashSet<>(8);
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
			}
			return result;
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * Resolve the given jar file URL into a JarFile object.
	 */
	protected JarFile getJarFile(String jarFileUrl) throws IOException {
		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			try {
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		}
		else {
			return new JarFile(jarFileUrl);
		}
	}

	/**
	 * Find all resources in the file system that match the given location pattern
	 * via the Ant-style PathMatcher.
	 * @param rootDirResource the root directory as Resource
	 * @param subPattern the sub pattern to match (below the root directory)
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException in case of I/O errors
	 * @see #retrieveMatchingFiles
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		File rootDir;
		try {
			rootDir = rootDirResource.getFile().getAbsoluteFile();
		}
		catch (IOException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching files underneath " + rootDirResource +
						" because it does not correspond to a directory in the file system", ex);
			}
			return Collections.emptySet();
		}
		return doFindMatchingFileSystemResources(rootDir, subPattern);
	}

	/**
	 * Find all resources in the file system that match the given location pattern
	 * via the Ant-style PathMatcher.
	 * @param rootDir the root directory in the file system
	 * @param subPattern the sub pattern to match (below the root directory)
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException in case of I/O errors
	 * @see #retrieveMatchingFiles
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
		}
		Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
		Set<Resource> result = new LinkedHashSet<>(matchingFiles.size());
		for (File file : matchingFiles) {
			result.add(new FileSystemResource(file));
		}
		return result;
	}

	/**
	 * Retrieve files that match the given path pattern,
	 * checking the given directory and its subdirectories.
	 * @param rootDir the directory to start from
	 * @param pattern the pattern to match against,
	 * relative to the root directory
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException if directory contents could not be retrieved
	 */
	protected Set<File> retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
		if (!rootDir.exists()) {
			// Silently skip non-existing directories.
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
			}
			return Collections.emptySet();
		}
		if (!rootDir.isDirectory()) {
			// Complain louder if it exists but is no directory.
			if (logger.isWarnEnabled()) {
				logger.warn("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
			}
			return Collections.emptySet();
		}
		if (!rootDir.canRead()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching files underneath directory [" + rootDir.getAbsolutePath() +
						"] because the application is not allowed to read the directory");
			}
			return Collections.emptySet();
		}
		String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
		if (!pattern.startsWith("/")) {
			fullPattern += "/";
		}
		fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
		Set<File> result = new LinkedHashSet<>(8);
		doRetrieveMatchingFiles(fullPattern, rootDir, result);
		return result;
	}

	/**
	 * Recursively retrieve files that match the given pattern,
	 * adding them to the given result list.
	 * @param fullPattern the pattern to match against,
	 * with prepended root directory path
	 * @param dir the current directory
	 * @param result the Set of matching File instances to add to
	 * @throws IOException if directory contents could not be retrieved
	 */
	protected void doRetrieveMatchingFiles(String fullPattern, File dir, Set<File> result) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching directory [" + dir.getAbsolutePath() +
					"] for files matching pattern [" + fullPattern + "]");
		}
		File[] dirContents = dir.listFiles();
		if (dirContents == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
			}
			return;
		}
		Arrays.sort(dirContents);
		for (File content : dirContents) {
			String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
			if (content.isDirectory() && getPathMatcher().matchStart(fullPattern, currPath + "/")) {
				if (!content.canRead()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() +
								"] because the application is not allowed to read the directory");
					}
				}
				else {
					doRetrieveMatchingFiles(fullPattern, content, result);
				}
			}
			if (getPathMatcher().match(fullPattern, currPath)) {
				result.add(content);
			}
		}
	}


	/**
	 * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
	 */
	private static class VfsResourceMatchingDelegate {

		public static Set<Resource> findMatchingResources(
				URL rootDirURL, String locationPattern, PathMatcher pathMatcher) throws IOException {

			Object root = VfsPatternUtils.findRoot(rootDirURL);
			PatternVirtualFileVisitor visitor =
					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
			VfsPatternUtils.visit(root, visitor);
			return visitor.getResources();
		}
	}


	/**
	 * VFS visitor for path matching purposes.
	 */
	@SuppressWarnings("unused")
	private static class PatternVirtualFileVisitor implements InvocationHandler {

		private final String subPattern;

		private final PathMatcher pathMatcher;

		private final String rootPath;

		private final Set<Resource> resources = new LinkedHashSet<>();

		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = (rootPath.length() == 0 || rootPath.endsWith("/") ? rootPath : rootPath + "/");
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (Object.class == method.getDeclaringClass()) {
				if (methodName.equals("equals")) {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				else if (methodName.equals("hashCode")) {
					return System.identityHashCode(proxy);
				}
			}
			else if ("getAttributes".equals(methodName)) {
				return getAttributes();
			}
			else if ("visit".equals(methodName)) {
				visit(args[0]);
				return null;
			}
			else if ("toString".equals(methodName)) {
				return toString();
			}

			throw new IllegalStateException("Unexpected method invocation: " + method);
		}

		public void visit(Object vfsResource) {
			if (this.pathMatcher.match(this.subPattern,
					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
				this.resources.add(new VfsResource(vfsResource));
			}
		}

		public Object getAttributes() {
			return VfsPatternUtils.getVisitorAttribute();
		}

		public Set<Resource> getResources() {
			return this.resources;
		}

		public int size() {
			return this.resources.size();
		}

		@Override
		public String toString() {
			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
		}
	}

}
