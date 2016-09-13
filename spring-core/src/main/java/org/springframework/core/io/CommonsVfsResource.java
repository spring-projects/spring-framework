/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.core.io;

/**
 *
 * @author Petar Tahchiev
 * @since 4.3
 */
public class CommonsVfsResource extends AbstractResource {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private FileObject fileObject;

    public CommonsVfsResource(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    @Override
    public boolean exists() {
        try {
            return this.fileObject.exists();
        } catch (FileSystemException ex) {
            LOG.error("Failed to invoke exists for file " + this.fileObject, ex);
            return false;
        }
    }

    @Override
    public boolean isReadable() {
        try {
            return this.fileObject.isReadable();
        } catch (FileSystemException ex) {
            LOG.error("Failed to invoke isReadable for file " + this.fileObject, ex);
            return false;
        }
    }

    @Override
    public URL getURL() throws IOException {
        try {
            return this.fileObject.getURL();
        } catch (Exception ex) {
            throw new NestedIOException("Failed to obtain URL for file " + this.fileObject, ex);
        }
    }

    @Override
    public URI getURI() throws IOException {
        try {
            return this.fileObject.getURL().toURI();
        } catch (Exception ex) {
            throw new NestedIOException("Failed to obtain URI for " + this.fileObject, ex);
        }
    }

    @Override
    public long contentLength() throws IOException {
        return IOUtils.toByteArray(this.fileObject.getContent().getInputStream()).length;
    }

    @Override
    public long lastModified() throws IOException {
        return this.fileObject.getContent().getLastModifiedTime();
    }

    @Override
    public String getFilename() {
        return this.fileObject.getName().getPath();
    }

    @Override
    public String getDescription() {
        return "Commons VFS resource [" + this.fileObject + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this || (obj instanceof CommonsVfsResource && this.fileObject.equals(((CommonsVfsResource) obj).fileObject))); //NOPMD
    }

    @Override
    public int hashCode() {
        return this.fileObject.hashCode();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return fileObject.getContent().getInputStream();
    }
}
