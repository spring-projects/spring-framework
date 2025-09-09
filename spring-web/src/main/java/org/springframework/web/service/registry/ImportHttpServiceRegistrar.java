/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.service.registry;

import java.util.Arrays;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * Built-in implementation of {@link AbstractHttpServiceRegistrar} that uses
 * {@link ImportHttpServices} annotations on the importing configuration class
 * to determine the HTTP services and groups to register.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 */
class ImportHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	private @Nullable MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {

		MergedAnnotation<?> groupsAnnot = metadata.getAnnotations().get(ImportHttpServices.Container.class);
		if (groupsAnnot.isPresent()) {
			for (MergedAnnotation<?> annot : groupsAnnot.getAnnotationArray("value", ImportHttpServices.class)) {
				processImportAnnotation(annot, registry, metadata);
			}
		}

		metadata.getAnnotations().stream(ImportHttpServices.class)
				.forEach(annot -> processImportAnnotation(annot, registry, metadata));
	}

	private void processImportAnnotation(MergedAnnotation<?> annotation, GroupRegistry groupRegistry,
			AnnotationMetadata metadata) {

		ImportHttpServices.GroupProvider groupProvider = getGroupProvider(annotation);
		HttpServiceGroup.ClientType clientType = annotation.getEnum("clientType", HttpServiceGroup.ClientType.class);
		Class<?>[] types = annotation.getClassArray("types");
		Class<?>[] basePackageClasses = annotation.getClassArray("basePackageClasses");
		String[] basePackages = annotation.getStringArray("basePackages");

		if (ObjectUtils.isEmpty(types) && ObjectUtils.isEmpty(basePackages) && ObjectUtils.isEmpty(basePackageClasses)) {
			basePackages = new String[] { ClassUtils.getPackageName(metadata.getClassName()) };
		}

		registerHttpServices(groupRegistry, groupProvider, clientType, types, basePackageClasses, basePackages);
	}

	private ImportHttpServices.GroupProvider getGroupProvider(MergedAnnotation<?> annotation) {
		String group = annotation.getString("group");
		Class<?> groupProvider = annotation.getClass("groupProvider");
		if (groupProvider == ImportHttpServices.GroupProvider.class) {
			return new FixedGroupProvider(StringUtils.hasText(group) ? group : HttpServiceGroup.DEFAULT_GROUP_NAME);
		}
		Assert.state(!StringUtils.hasText(group), "'group' cannot be mixed with 'groupProvider'");
		return (ImportHttpServices.GroupProvider) BeanUtils.instantiateClass(groupProvider);
	}

	private void registerHttpServices(GroupRegistry groupRegistry,
			ImportHttpServices.GroupProvider groupProvider, ClientType clientType, Class<?>[] types,
			Class<?>[] basePackageClasses, String[] basePackages) {

		if (groupProvider instanceof FixedGroupProvider fixedGroupProvider) {
			String groupName = fixedGroupProvider.group();
			groupRegistry.forGroup(groupName, clientType)
					.register(types)
					.detectInBasePackages(basePackageClasses)
					.detectInBasePackages(basePackages);
		}
		else {
			MetadataReaderFactory metadataReaderFactory = (this.metadataReaderFactory != null) ?
					this.metadataReaderFactory : new CachingMetadataReaderFactory();

			Consumer<AnnotationMetadata> register = metadata -> {
				String group = groupProvider.group(metadata);
				if (group != null) {
					groupRegistry.forGroup(group, clientType).registerTypeNames(metadata.getClassName());
				}
			};

			Arrays.stream(types)
					.map(Class::getName)
					.map(ThrowingFunction.of(metadataReaderFactory::getMetadataReader))
					.map(MetadataReader::getAnnotationMetadata)
					.forEach(register);
			Arrays.stream(basePackageClasses)
					.map(Class::getPackageName)
					.flatMap(this::findHttpServices)
					.map(this::getMetadata)
					.forEach(register);
			Arrays.stream(basePackages)
					.flatMap(this::findHttpServices)
					.map(this::getMetadata)
					.forEach(register);
		}
	}

	private AnnotationMetadata getMetadata(BeanDefinition beanDefinition) {
		Assert.state(beanDefinition instanceof AnnotatedBeanDefinition,
				"AnnotatedBeanDefinition required when using 'groupProvider'");
		return ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
	}

	private static record FixedGroupProvider(String group) implements ImportHttpServices.GroupProvider {

		@Override
		public String group(AnnotationMetadata metadata) {
			return this.group;
		}
	}

}
