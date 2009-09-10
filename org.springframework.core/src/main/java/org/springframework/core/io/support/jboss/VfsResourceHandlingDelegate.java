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

package org.springframework.core.io.support.jboss;

import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.LinkedHashSet;
import java.net.URL;
import java.net.URI;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileVisitor;
import org.jboss.virtual.VisitorAttributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceHandlingDelegate;
import org.springframework.util.PathMatcher;

/**
 * {@link org.springframework.core.io.support.ResourceHandlingDelegate} implementation
 * for JBoss' Virtual File System.
 *
 * @author Marius Bogoevici
 * @author Ales Justin
 *
 * Note: Thanks to David Ward from Alfresco for indicating a fix for path matching. 
 */
public class VfsResourceHandlingDelegate implements ResourceHandlingDelegate {

	public boolean canHandleResource(URL url) {
		return url.getProtocol().startsWith("vfs");
	}

	public boolean canHandleResource(URI uri) {
		return uri.getScheme().startsWith("vfs");
	}


	public Set<Resource> findMatchingResources(Resource rootResource, String locationPattern, PathMatcher pathMatcher) throws IOException {
        VirtualFile root = VFS.getRoot(rootResource.getURL());
        PatternVirtualFileVisitor visitor = new PatternVirtualFileVisitor(root.getPathName(), locationPattern, pathMatcher);
        root.visit(visitor);
        return visitor.getResources();
	}

	public Resource loadResource(URL url) throws IOException{
		return new VfsResource(VFS.getRoot(url));
	}

	public Resource loadResource(URI uri) throws IOException {
		return new VfsResource(VFS.getRoot(uri));
	}


	protected static class PatternVirtualFileVisitor implements VirtualFileVisitor
	{
		private final String subPattern;
		private final Set<Resource> resources = new LinkedHashSet<Resource>();
		private final PathMatcher pathMatcher;
		private final String rootPath;

		private PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher)
		{
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = rootPath.length() == 0 || rootPath.endsWith("/") ? rootPath : rootPath + "/";
		}

		public VisitorAttributes getAttributes()
		{
			return VisitorAttributes.RECURSE;
		}

		public void visit(VirtualFile vf)
		{
			if (pathMatcher.match(subPattern, vf.getPathName().substring(rootPath.length())))
				resources.add(new VfsResource(vf));
		}

		public Set<Resource> getResources()
		{
			return resources;
		}

		public int size()
		{
			return resources.size();
		}

		public String toString()
		{
			StringBuffer buffer = new StringBuffer();
			buffer.append("sub-pattern: ").append(subPattern);
			buffer.append(", resources: ").append(resources);
			return buffer.toString();
		}
	}

}
