/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class for JAXB2.
 *
 * @author Arjen Poutsma
 * @since 6.1
 */
abstract class Jaxb2Helper {

	/**
	 * The default value for JAXB annotations.
	 * @see XmlRootElement#name()
	 * @see XmlRootElement#namespace()
	 * @see XmlType#name()
	 * @see XmlType#namespace()
	 */
	private static final String JAXB_DEFAULT_ANNOTATION_VALUE = "##default";


	/**
	 * Returns the set of qualified names for the given class, according to the
	 * mapping rules in the JAXB specification.
	 */
	public static Set<QName> toQNames(Class<?> clazz) {
		Set<QName> result = new HashSet<>(1);
		findQNames(clazz, result, new HashSet<>());
		return result;
	}

	private static void findQNames(Class<?> clazz, Set<QName> qNames, Set<Class<?>> completedClasses) {
		// safety against circular XmlSeeAlso references
		if (completedClasses.contains(clazz)) {
			return;
		}
		if (clazz.isAnnotationPresent(XmlRootElement.class)) {
			XmlRootElement annotation = clazz.getAnnotation(XmlRootElement.class);
			qNames.add(new QName(namespace(annotation.namespace(), clazz),
					localPart(annotation.name(), clazz)));
		}
		else if (clazz.isAnnotationPresent(XmlType.class)) {
			XmlType annotation = clazz.getAnnotation(XmlType.class);
			qNames.add(new QName(namespace(annotation.namespace(), clazz),
					localPart(annotation.name(), clazz)));
		}
		else {
			throw new IllegalArgumentException("Output class [" + clazz.getName() +
					"] is neither annotated with @XmlRootElement nor @XmlType");
		}
		completedClasses.add(clazz);
		if (clazz.isAnnotationPresent(XmlSeeAlso.class)) {
			XmlSeeAlso annotation = clazz.getAnnotation(XmlSeeAlso.class);
			for (Class<?> seeAlso : annotation.value()) {
				findQNames(seeAlso, qNames, completedClasses);
			}
		}
	}

	private static String localPart(String value, Class<?> outputClass) {
		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(value)) {
			return ClassUtils.getShortNameAsProperty(outputClass);
		}
		else {
			return value;
		}
	}

	private static String namespace(String value, Class<?> outputClass) {
		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(value)) {
			Package outputClassPackage = outputClass.getPackage();
			if (outputClassPackage != null && outputClassPackage.isAnnotationPresent(XmlSchema.class)) {
				XmlSchema annotation = outputClassPackage.getAnnotation(XmlSchema.class);
				return annotation.namespace();
			}
			else {
				return XMLConstants.NULL_NS_URI;
			}
		}
		else {
			return value;
		}
	}

	/**
	 * Split a flux of {@link XMLEvent XMLEvents} into a flux of XMLEvent lists, one list
	 * for each branch of the tree that starts with one of the given qualified names.
	 * That is, given the XMLEvents shown {@linkplain XmlEventDecoder here},
	 * and the name "{@code child}", this method returns a flux
	 * of two lists, each of which containing the events of a particular branch
	 * of the tree that starts with "{@code child}".
	 * <ol>
	 * <li>The first list, dealing with the first branch of the tree:
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * <li>The second list, dealing with the second branch of the tree:
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * </li>
	 * </ol>
	 */
	public static Flux<List<XMLEvent>> split(Flux<XMLEvent> xmlEventFlux, Set<QName> names) {
		return xmlEventFlux.handle(new SplitHandler(names));
	}


	private static class SplitHandler implements BiConsumer<XMLEvent, SynchronousSink<List<XMLEvent>>> {

		private final Set<QName> names;

		@Nullable
		private List<XMLEvent> events;

		private int elementDepth = 0;

		private int barrier = Integer.MAX_VALUE;

		public SplitHandler(Set<QName> names) {
			this.names = names;
		}

		@Override
		public void accept(XMLEvent event, SynchronousSink<List<XMLEvent>> sink) {
			if (event.isStartElement()) {
				if (this.barrier == Integer.MAX_VALUE) {
					QName startElementName = event.asStartElement().getName();
					if (this.names.contains(startElementName)) {
						this.events = new ArrayList<>();
						this.barrier = this.elementDepth;
					}
				}
				this.elementDepth++;
			}
			if (this.elementDepth > this.barrier) {
				Assert.state(this.events != null, "No XMLEvent List");
				this.events.add(event);
			}
			if (event.isEndElement()) {
				this.elementDepth--;
				if (this.elementDepth == this.barrier) {
					this.barrier = Integer.MAX_VALUE;
					Assert.state(this.events != null, "No XMLEvent List");
					sink.next(this.events);
				}
			}
		}
	}


}
