/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.index.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.ManagedBean;
import javax.inject.Named;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.index.sample.AbstractController;
import org.springframework.context.index.sample.MetaControllerIndexed;
import org.springframework.context.index.sample.SampleComponent;
import org.springframework.context.index.sample.SampleController;
import org.springframework.context.index.sample.SampleEmbedded;
import org.springframework.context.index.sample.SampleMetaController;
import org.springframework.context.index.sample.SampleMetaIndexedController;
import org.springframework.context.index.sample.SampleNonStaticEmbedded;
import org.springframework.context.index.sample.SampleNone;
import org.springframework.context.index.sample.SampleRepository;
import org.springframework.context.index.sample.SampleService;
import org.springframework.context.index.sample.cdi.SampleManagedBean;
import org.springframework.context.index.sample.cdi.SampleNamed;
import org.springframework.context.index.sample.cdi.SampleTransactional;
import org.springframework.context.index.sample.jpa.SampleConverter;
import org.springframework.context.index.sample.jpa.SampleEmbeddable;
import org.springframework.context.index.sample.jpa.SampleEntity;
import org.springframework.context.index.sample.jpa.SampleMappedSuperClass;
import org.springframework.context.index.sample.type.Repo;
import org.springframework.context.index.sample.type.SampleRepo;
import org.springframework.context.index.sample.type.SampleSmartRepo;
import org.springframework.context.index.sample.type.SampleSpecializedRepo;
import org.springframework.context.index.sample.type.SmartRepo;
import org.springframework.context.index.sample.type.SpecializedRepo;
import org.springframework.context.index.test.TestCompiler;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CandidateComponentsIndexer}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Sam Brannen
 */
class CandidateComponentsIndexerTests {

	private TestCompiler compiler;


	@BeforeEach
	void createCompiler(@TempDir Path tempDir) throws IOException {
		this.compiler = new TestCompiler(tempDir);
	}

	@Test
	void noCandidate() {
		CandidateComponentsMetadata metadata = compile(SampleNone.class);
		assertThat(metadata.getItems()).hasSize(0);
	}

	@Test
	void noAnnotation() {
		CandidateComponentsMetadata metadata = compile(CandidateComponentsIndexerTests.class);
		assertThat(metadata.getItems()).hasSize(0);
	}

	@Test
	void stereotypeComponent() {
		testComponent(SampleComponent.class);
	}

	@Test
	void stereotypeService() {
		testComponent(SampleService.class);
	}

	@Test
	void stereotypeController() {
		testComponent(SampleController.class);
	}

	@Test
	void stereotypeControllerMetaAnnotation() {
		testComponent(SampleMetaController.class);
	}

	@Test
	void stereotypeRepository() {
		testSingleComponent(SampleRepository.class, Component.class);
	}

	@Test
	void stereotypeControllerMetaIndex() {
		testSingleComponent(SampleMetaIndexedController.class, Component.class, MetaControllerIndexed.class);
	}

	@Test
	void stereotypeOnAbstractClass() {
		testComponent(AbstractController.class);
	}

	@Test
	void cdiManagedBean() {
		testSingleComponent(SampleManagedBean.class, ManagedBean.class);
	}

	@Test
	void cdiNamed() {
		testSingleComponent(SampleNamed.class, Named.class);
	}

	@Test
	void cdiTransactional() {
		testSingleComponent(SampleTransactional.class, Transactional.class);
	}

	@Test
	void persistenceEntity() {
		testSingleComponent(SampleEntity.class, Entity.class);
	}

	@Test
	void persistenceMappedSuperClass() {
		testSingleComponent(SampleMappedSuperClass.class, MappedSuperclass.class);
	}

	@Test
	void persistenceEmbeddable() {
		testSingleComponent(SampleEmbeddable.class, Embeddable.class);
	}

	@Test
	void persistenceConverter() {
		testSingleComponent(SampleConverter.class, Converter.class);
	}

	@Test
	void packageInfo() {
		CandidateComponentsMetadata metadata = compile("org/springframework/context/index/sample/jpa/package-info");
		assertThat(metadata).has(Metadata.of("org.springframework.context.index.sample.jpa", "package-info"));
	}

	@Test
	void typeStereotypeFromMetaInterface() {
		testSingleComponent(SampleSpecializedRepo.class, Repo.class);
	}

	@Test
	void typeStereotypeFromInterfaceFromSuperClass() {
		testSingleComponent(SampleRepo.class, Repo.class);
	}

	@Test
	void typeStereotypeFromSeveralInterfaces() {
		testSingleComponent(SampleSmartRepo.class, Repo.class, SmartRepo.class);
	}

	@Test
	void typeStereotypeOnInterface() {
		testSingleComponent(SpecializedRepo.class, Repo.class);
	}

	@Test
	void typeStereotypeOnInterfaceFromSeveralInterfaces() {
		testSingleComponent(SmartRepo.class, Repo.class, SmartRepo.class);
	}

	@Test
	void typeStereotypeOnIndexedInterface() {
		testSingleComponent(Repo.class, Repo.class);
	}

	@Test
	void embeddedCandidatesAreDetected()
			throws IOException, ClassNotFoundException {
		// Validate nested type structure
		String nestedType = "org.springframework.context.index.sample.SampleEmbedded.Another$AnotherPublicCandidate";
		Class<?> type = ClassUtils.forName(nestedType, getClass().getClassLoader());
		assertThat(type).isSameAs(SampleEmbedded.Another.AnotherPublicCandidate.class);

		CandidateComponentsMetadata metadata = compile(SampleEmbedded.class);
		assertThat(metadata).has(Metadata.of(SampleEmbedded.PublicCandidate.class, Component.class));
		assertThat(metadata).has(Metadata.of(nestedType, Component.class.getName()));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void embeddedNonStaticCandidateAreIgnored() {
		CandidateComponentsMetadata metadata = compile(SampleNonStaticEmbedded.class);
		assertThat(metadata.getItems()).hasSize(0);
	}

	private void testComponent(Class<?>... classes) {
		CandidateComponentsMetadata metadata = compile(classes);
		for (Class<?> c : classes) {
			assertThat(metadata).has(Metadata.of(c, Component.class));
		}
		assertThat(metadata.getItems()).hasSize(classes.length);
	}

	private void testSingleComponent(Class<?> target, Class<?>... stereotypes) {
		CandidateComponentsMetadata metadata = compile(target);
		assertThat(metadata).has(Metadata.of(target, stereotypes));
		assertThat(metadata.getItems()).hasSize(1);
	}

	private CandidateComponentsMetadata compile(Class<?>... types) {
		CandidateComponentsIndexer processor = new CandidateComponentsIndexer();
		this.compiler.getTask(types).call(processor);
		return readGeneratedMetadata(this.compiler.getOutputLocation());
	}

	private CandidateComponentsMetadata compile(String... types) {
		CandidateComponentsIndexer processor = new CandidateComponentsIndexer();
		this.compiler.getTask(types).call(processor);
		return readGeneratedMetadata(this.compiler.getOutputLocation());
	}

	private CandidateComponentsMetadata readGeneratedMetadata(File outputLocation) {
		File metadataFile = new File(outputLocation, MetadataStore.METADATA_PATH);
		if (metadataFile.isFile()) {
			try (FileInputStream fileInputStream = new FileInputStream(metadataFile)) {
				CandidateComponentsMetadata metadata = PropertiesMarshaller.read(fileInputStream);
				return metadata;
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to read metadata from disk", ex);
			}
		}
		else {
			return new CandidateComponentsMetadata();
		}
	}

}
