/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.aspectj;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * An aspect that injects dependency into any object whose type implements the {@link ConfigurableObject} interface.
 * <p>
 * This aspect supports injecting into domain objects when they are created for the first time as well as
 * upon deserialization. Subaspects need to simply provide definition for the configureBean() method. This
 * method may be implemented without relying on Spring container if so desired.
 * </p>
 * <p>
 * There are two cases that needs to be handled:
 * <ol>
 *   <li>Normal object creation via the '<code>new</code>' operator: this is
 *       taken care of by advising <code>initialization()</code> join points.</li>
 *   <li>Object creation through deserialization: since no constructor is
 *       invoked during deserialization, the aspect needs to advise a method that a
 *       deserialization mechanism is going to invoke. Ideally, we should not
 *       require user classes to implement any specific method. This implies that
 *       we need to <i>introduce</i> the chosen method. We should also handle the cases
 *       where the chosen method is already implemented in classes (in which case,
 *       the user's implementation for that method should take precedence over the
 *       introduced implementation). There are a few choices for the chosen method:
 *       <ul>
 *       <li>readObject(ObjectOutputStream): Java requires that the method must be
 *           <code>private</p>. Since aspects cannot introduce a private member,
 *           while preserving its name, this option is ruled out.</li>
 * 		 <li>readResolve(): Java doesn't pose any restriction on an access specifier.
 *           Problem solved! There is one (minor) limitation of this approach in
 *           that if a user class already has this method, that method must be
 *           <code>public</code>. However, this shouldn't be a big burden, since
 *           use cases that need classes to implement readResolve() (custom enums,
 *           for example) are unlikely to be marked as &#64;Configurable, and
 *           in any case asking to make that method <code>public</code> should not
 *           pose any undue burden.</li>
 *       </ul>
 *       The minor collaboration needed by user classes (i.e., that the
 *       implementation of <code>readResolve()</code>, if any, must be
 *       <code>public</code>) can be lifted as well if we were to use an
 *       experimental feature in AspectJ - the <code>hasmethod()</code> PCD.</li>
 * </ol>

 * <p>
 * While having type implement the {@link ConfigurableObject} interface is certainly a valid choice, an alternative
 * is to use a 'declare parents' statement another aspect (a subaspect of this aspect would be a logical choice)
 * that declares the classes that need to be configured by supplying the {@link ConfigurableObject} interface.
 * </p>
 *
 * @author Ramnivas Laddad
 * @since 2.5.2
 */
public abstract aspect AbstractInterfaceDrivenDependencyInjectionAspect extends AbstractDependencyInjectionAspect {
	/**
	 * Select initialization join point as object construction
	 */
	public pointcut beanConstruction(Object bean) :
		initialization(ConfigurableObject+.new(..)) && this(bean);

	/**
	 * Select deserialization join point made available through ITDs for ConfigurableDeserializationSupport
	 */
	public pointcut beanDeserialization(Object bean) :
		execution(Object ConfigurableDeserializationSupport+.readResolve()) &&
		this(bean);

	public pointcut leastSpecificSuperTypeConstruction() : initialization(ConfigurableObject.new(..));



	// Implementation to support re-injecting dependencies once an object is deserialized
	/**
	 * Declare any class implementing Serializable and ConfigurableObject as also implementing
	 * ConfigurableDeserializationSupport. This allows us to introduce the readResolve()
	 * method and select it with the beanDeserialization() pointcut.
	 *
	 * <p>Here is an improved version that uses the hasmethod() pointcut and lifts
	 * even the minor requirement on user classes:
	 *
	 * <pre class="code">declare parents: ConfigurableObject+ Serializable+
	 *		            && !hasmethod(Object readResolve() throws ObjectStreamException)
	 *		            implements ConfigurableDeserializationSupport;
	 * </pre>
	 */
	declare parents:
		ConfigurableObject+ && Serializable+	implements ConfigurableDeserializationSupport;

	/**
	 * A marker interface to which the <code>readResolve()</code> is introduced.
	 */
	static interface ConfigurableDeserializationSupport extends Serializable {
	}

	/**
	 * Introduce the <code>readResolve()</code> method so that we can advise its
	 * execution to configure the object.
	 *
	 * <p>Note if a method with the same signature already exists in a
	 * <code>Serializable</code> class of ConfigurableObject type,
	 * that implementation will take precedence (a good thing, since we are
	 * merely interested in an opportunity to detect deserialization.)
	 */
	public Object ConfigurableDeserializationSupport.readResolve() throws ObjectStreamException {
		return this;
	}

}
