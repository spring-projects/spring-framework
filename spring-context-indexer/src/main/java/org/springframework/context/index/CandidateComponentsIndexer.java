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

package org.springframework.context.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.context.index.metadata.CandidateComponentsMetadata;
import org.springframework.context.index.metadata.ItemMetadata;

/**
 * Annotation {@link Processor} that writes {@link CandidateComponentsMetadata}
 * file for spring components.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CandidateComponentsIndexer extends AbstractProcessor {

	private MetadataStore metadataStore;

	private MetadataCollector metadataCollector;

	private TypeUtils typeUtils;

	private List<StereotypesProvider> stereotypesProviders;

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		this.stereotypesProviders = getStereotypesProviders(env);
		this.typeUtils = new TypeUtils(env);
		this.metadataStore = new MetadataStore(env);
		this.metadataCollector = new MetadataCollector(env,
				this.metadataStore.readMetadata());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.metadataCollector.processing(roundEnv);
		roundEnv.getRootElements().forEach(this::processElement);

		if (roundEnv.processingOver()) {
			writeMetaData();
		}
		return false;
	}

	protected List<StereotypesProvider> getStereotypesProviders(ProcessingEnvironment env) {
		List<StereotypesProvider> result = new ArrayList<>();
		TypeUtils typeUtils = new TypeUtils(env);
		result.add(new IndexedStereotypesProvider(typeUtils));
		result.add(new StandardStereotypesProvider(typeUtils));
		result.add(new PackageInfoStereotypesProvider());
		return result;
	}

	private void processElement(Element element) {
		Set<String> stereotypes = new LinkedHashSet<>();
		this.stereotypesProviders.forEach(p -> {
			stereotypes.addAll(p.getStereotypes(element));

		});
		if (!stereotypes.isEmpty()) {
			this.metadataCollector.add(new ItemMetadata(
					this.typeUtils.getType(element), stereotypes));
		}
	}

	protected CandidateComponentsMetadata writeMetaData() {
		CandidateComponentsMetadata metadata = this.metadataCollector.getMetadata();
		if (!metadata.getItems().isEmpty()) {
			try {
				this.metadataStore.writeMetadata(metadata);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
			return metadata;
		}
		return null;
	}

}
