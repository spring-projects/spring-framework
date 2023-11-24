/*
 * Copyright 2002,2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.proxy;

/**
 * All enhanced instances returned by the {@link Enhancer} class implement this interface.
 * Using this interface for new instances is faster than going through the <code>Enhancer</code>
 * interface or using reflection. In addition, to intercept methods called during
 * object construction you <b>must</b> use these methods instead of reflection.
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt">baliuka@mwm.lt</a>
 * @version $Id: Factory.java,v 1.13 2004/06/24 21:15:20 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface Factory {
    /**
     * Creates new instance of the same type, using the no-arg constructor.
     * The class of this object must have been created using a single Callback type.
     * If multiple callbacks are required an exception will be thrown.
     * @param callback the new interceptor to use
     * @return new instance of the same type
     */
    Object newInstance(Callback callback);

    /**
     * Creates new instance of the same type, using the no-arg constructor.
     * @param callbacks the new callbacks(s) to use
     * @return new instance of the same type
     */
    Object newInstance(Callback[] callbacks);

    /**
     * Creates a new instance of the same type, using the constructor
     * matching the given signature.
     * @param types the constructor argument types
     * @param args the constructor arguments
     * @param callbacks the new interceptor(s) to use
     * @return new instance of the same type
     */
    Object newInstance(Class[] types, Object[] args, Callback[] callbacks);

    /**
     * Return the <code>Callback</code> implementation at the specified index.
     * @param index the callback index
     * @return the callback implementation
     */
    Callback getCallback(int index);

    /**
     * Set the callback for this object for the given type.
     * @param index the callback index to replace
     * @param callback the new callback
     */
    void setCallback(int index, Callback callback);

    /**
     * Replace all of the callbacks for this object at once.
     * @param callbacks the new callbacks(s) to use
     */
    void setCallbacks(Callback[] callbacks);

    /**
     * Get the current set of callbacks for ths object.
     * @return a new array instance
     */
    Callback[] getCallbacks();
}
