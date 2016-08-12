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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.annotation.ManagedBean;
import javax.inject.Named;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.context.index.metadata.CandidateComponentsMetadata;
import org.springframework.context.index.metadata.PropertiesMarshaller;
import org.springframework.context.index.sample.AbstractController;
import org.springframework.context.index.sample.MetaControllerIndexed;
import org.springframework.context.index.sample.SampleComponent;
import org.springframework.context.index.sample.SampleController;
import org.springframework.context.index.sample.SampleMetaController;
import org.springframework.context.index.sample.SampleMetaIndexedController;
import org.springframework.context.index.sample.SampleNone;
import org.springframework.context.index.sample.SampleRepository;
import org.springframework.context.index.sample.SampleService;
import org.springframework.context.index.sample.cdi.SampleManagedBean;
import org.springframework.context.index.sample.cdi.SampleNamed;
import org.springframework.context.index.sample.jpa.SampleConverter;
import org.springframework.context.index.sample.jpa.SampleEmbeddable;
import org.springframework.context.index.sample.jpa.SampleEntity;
import org.springframework.context.index.sample.jpa.SampleMappedSuperClass;
import org.springframework.context.index.sample.type.SampleRepo;
import org.springframework.context.index.sample.type.SampleSmartRepo;
import org.springframework.context.index.sample.type.SampleSpecializedRepo;
import org.springframework.context.index.sample.type.Repo;
import org.springframework.context.index.sample.type.SmartRepo;
import org.springframework.context.index.sample.type.SpecializedRepo;
import org.springframework.context.index.test.TestCompiler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.context.index.test.Metadata.*;

/**
 * Tests for {@link CandidateComponentsIndexer}.
 *
 * @author Stephane Nicoll
 */
public class CandidateComponentsIndexerTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TestCompiler compiler;

	@Before
	public void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.temporaryFolder);
	}

	@Test
	public void noCandidate() throws IOException {
		CandidateComponentsMetadata metadata = compile(SampleNone.class);
		assertThat(metadata.getItems(), hasSize(0));
	}

	@Test
	public void noAnnotation() throws IOException {
		CandidateComponentsMetadata metadata = compile(CandidateComponentsIndexerTests.class);
		assertThat(metadata.getItems(), hasSize(0));
	}

	@Test
	public void stereotypeComponent() throws IOException {
		testComponent(SampleComponent.class);
	}

	@Test
	public void stereotypeService() throws IOException {
		testComponent(SampleService.class);
	}

	@Test
	public void stereotypeController() throws IOException {
		testComponent(SampleController.class);
	}

	@Test
	public void stereotypeControllerMetaAnnotation() throws IOException {
		testComponent(SampleMetaController.class);
	}

	@Test
	public void stereotypeRepository() throws IOException {
		testSingleComponent(SampleRepository.class, Component.class);
	}

	@Test
	public void stereotypeControllerMetaIndex() throws IOException {
		testSingleComponent(SampleMetaIndexedController.class,
				Component.class, MetaControllerIndexed.class);
	}

	@Test
	public void stereotypeOnAbstractClass() throws IOException {
		testComponent(AbstractController.class);
	}

	@Test
	public void cdiManagedBean() throws IOException {
		testSingleComponent(SampleManagedBean.class, ManagedBean.class);
	}

	@Test
	public void cdiNamed() throws IOException {
		testSingleComponent(SampleNamed.class, Named.class);
	}

	@Test
	public void persistenceEntity() throws IOException {
		testSingleComponent(SampleEntity.class, Entity.class);
	}

	@Test
	public void persistenceMappedSuperClass() throws IOException {
		testSingleComponent(SampleMappedSuperClass.class, MappedSuperclass.class);
	}

	@Test
	public void persistenceEmbeddable() throws IOException {
		testSingleComponent(SampleEmbeddable.class, Embeddable.class);
	}

	@Test
	public void persistenceConverter() throws IOException {
		testSingleComponent(SampleConverter.class, Converter.class);
	}

	@Test
	public void packageInfo() throws IOException {
		CandidateComponentsMetadata metadata = compile(
				"org/springframework/context/index/sample/jpa/package-info");
		assertThat(metadata, hasComponent(
				"org.springframework.context.index.sample.jpa", "package-info"));
	}

	@Test
	public void typeStereotypeFromMetaInterface() throws IOException {
		testSingleComponent(SampleSpecializedRepo.class, Repo.class);
	}

	@Test
	public void typeStereotypeFromInterfaceFromSuperClass() throws IOException {
		testSingleComponent(SampleRepo.class, Repo.class);
	}

	@Test
	public void typeStereotypeFromSeveralInterfaces() throws IOException {
		testSingleComponent(SampleSmartRepo.class, Repo.class, SmartRepo.class);
	}

	@Test
	public void typeStereotypeOnInterface() throws IOException {
		testSingleComponent(SpecializedRepo.class, Repo.class);
	}

	@Test
	public void typeStereotypeOnInterfaceFromSeveralInterfaces() throws IOException {
		testSingleComponent(SmartRepo.class, Repo.class, SmartRepo.class);
	}

	@Test
	public void typeStereotypeOnIndexedInterface() throws IOException {
		testSingleComponent(Repo.class, Repo.class);
	}


	private void testComponent(Class<?>... classes) throws IOException {
		CandidateComponentsMetadata metadata = compile(classes);
		for (Class<?> c : classes) {
			assertThat(metadata, hasComponent(c, Component.class));
		}
		assertThat(metadata.getItems(), hasSize(classes.length));
	}

	private void testSingleComponent(Class<?> target, Class<?>... stereotypes) throws IOException {
		CandidateComponentsMetadata metadata = compile(target);
		assertThat(metadata, hasComponent(target, stereotypes));
		assertThat(metadata.getItems(), hasSize(1));
	}

	private CandidateComponentsMetadata compile(Class<?>... types) throws IOException {
		CandidateComponentsIndexer processor = new CandidateComponentsIndexer();
		this.compiler.getTask(types).call(processor);
		return readGeneratedMetadata(this.compiler.getOutputLocation());
	}

	private CandidateComponentsMetadata compile(String... types) throws IOException {
		CandidateComponentsIndexer processor = new CandidateComponentsIndexer();
		this.compiler.getTask(types).call(processor);
		return readGeneratedMetadata(this.compiler.getOutputLocation());
	}

	private CandidateComponentsMetadata readGeneratedMetadata(File outputLocation) {
		try {
			File metadataFile = new File(outputLocation,
					MetadataStore.METADATA_PATH);
			if (metadataFile.isFile()) {
				return new PropertiesMarshaller()
						.read(new FileInputStream(metadataFile));
			}
			else {
				return new CandidateComponentsMetadata();
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read metadata from disk", e);
		}
	}

}
