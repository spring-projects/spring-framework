package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;

/**
 * This interface implementation is designed to provide a facility to populate the
 * <code>ExtensionRegistry</code> with the appropriate protobuf message extensions.
 *
 * @author  Alex Antonov
 */
public interface ExtensionRegistryInitializer {
    public void initializeExtensionRegistry(ExtensionRegistry registry);
}
