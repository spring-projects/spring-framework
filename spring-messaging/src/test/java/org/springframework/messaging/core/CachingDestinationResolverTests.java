/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.messaging.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.springframework.messaging.core.CachingDestinationResolver}
 *
 * @author Agim Emruli
 */
public class CachingDestinationResolverTests {

    @Test
    public void cachedDestination() throws Exception {
        @SuppressWarnings("unchecked")
        DestinationResolver<String> destinationResolver = (DestinationResolver<String>) mock(DestinationResolver.class);
        CachingDestinationResolver<String> cachingDestinationResolver = new CachingDestinationResolver<String>(destinationResolver);

        when(destinationResolver.resolveDestination("abcd")).thenReturn("dcba");

        assertEquals("dcba",cachingDestinationResolver.resolveDestination("abcd"));
        assertEquals("dcba",cachingDestinationResolver.resolveDestination("abcd"));

        verify(destinationResolver, times(1)).resolveDestination("abcd");
    }

    @Test
    public void nonResolvedDestination() throws Exception {

        @SuppressWarnings("unchecked")
        DestinationResolver<String> destinationResolver = (DestinationResolver<String>) mock(DestinationResolver.class);
        CachingDestinationResolver<String> cachingDestinationResolver = new CachingDestinationResolver<String>(destinationResolver);

        assertNull(cachingDestinationResolver.resolveDestination("abcd"));
        assertNull(cachingDestinationResolver.resolveDestination("abcd"));

        verify(destinationResolver, times(2)).resolveDestination("abcd");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDelegate() throws Exception {
        new CachingDestinationResolver<Object>(null);
    }
}