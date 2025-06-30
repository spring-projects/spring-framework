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

package org.springframework.orm.jpa.persistenceunit;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring's base implementation of the JPA
 * {@link jakarta.persistence.spi.PersistenceUnitInfo} interface,
 * used to bootstrap an {@code EntityManagerFactory} in a container.
 *
 * <p>This implementation is largely a JavaBean, offering mutators
 * for all standard {@code PersistenceUnitInfo} properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 */
@SuppressWarnings("removal")
public class MutablePersistenceUnitInfo implements SmartPersistenceUnitInfo {

	private @Nullable String persistenceUnitName;

	private @Nullable String persistenceProviderClassName;

	private @Nullable String scopeAnnotationName;

	private final List<String> qualifierAnnotationNames = new ArrayList<>();

	private @Nullable PersistenceUnitTransactionType transactionType;

	private @Nullable DataSource nonJtaDataSource;

	private @Nullable DataSource jtaDataSource;

	private final List<String> mappingFileNames = new ArrayList<>();

	private final List<URL> jarFileUrls = new ArrayList<>();

	private @Nullable URL persistenceUnitRootUrl;

	private final List<String> managedClassNames = new ArrayList<>();

	private final List<String> managedPackages = new ArrayList<>();

	private boolean excludeUnlistedClasses = false;

	private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;

	private ValidationMode validationMode = ValidationMode.AUTO;

	private Properties properties = new Properties();

	private String persistenceXMLSchemaVersion = "3.2";

	private @Nullable String persistenceProviderPackageName;


	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	public @Nullable String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	public void setPersistenceProviderClassName(@Nullable String persistenceProviderClassName) {
		this.persistenceProviderClassName = persistenceProviderClassName;
	}

	@Override
	public @Nullable String getPersistenceProviderClassName() {
		return this.persistenceProviderClassName;
	}

	public void setScopeAnnotationName(@Nullable String scopeAnnotationName) {
		this.scopeAnnotationName = scopeAnnotationName;
	}

	@Override
	public @Nullable String getScopeAnnotationName() {
		return this.scopeAnnotationName;
	}

	public void addQualifierAnnotationName(String qualifierAnnotationName) {
		this.qualifierAnnotationNames.add(qualifierAnnotationName);
	}

	@Override
	public List<String> getQualifierAnnotationNames() {
		return this.qualifierAnnotationNames;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		if (this.transactionType != null) {
			return this.transactionType;
		}
		else {
			return (this.jtaDataSource != null ?
					PersistenceUnitTransactionType.JTA : PersistenceUnitTransactionType.RESOURCE_LOCAL);
		}
	}

	public void setJtaDataSource(@Nullable DataSource jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	@Override
	public @Nullable DataSource getJtaDataSource() {
		return this.jtaDataSource;
	}

	public void setNonJtaDataSource(@Nullable DataSource nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	@Override
	public @Nullable DataSource getNonJtaDataSource() {
		return this.nonJtaDataSource;
	}

	public void addMappingFileName(String mappingFileName) {
		this.mappingFileNames.add(mappingFileName);
	}

	@Override
	public List<String> getMappingFileNames() {
		return this.mappingFileNames;
	}

	public void addJarFileUrl(URL jarFileUrl) {
		this.jarFileUrls.add(jarFileUrl);
	}

	@Override
	public List<URL> getJarFileUrls() {
		return this.jarFileUrls;
	}

	public void setPersistenceUnitRootUrl(@Nullable URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	@Override
	public @Nullable URL getPersistenceUnitRootUrl() {
		return this.persistenceUnitRootUrl;
	}

	/**
	 * Add a managed class name to the persistence provider's metadata.
	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
	 * @see #addManagedPackage
	 */
	public void addManagedClassName(String managedClassName) {
		this.managedClassNames.add(managedClassName);
	}

	@Override
	public List<String> getManagedClassNames() {
		return this.managedClassNames;
	}

	/**
	 * Add a managed package to the persistence provider's metadata.
	 * <p>Note: This refers to annotated {@code package-info.java} files. It does
	 * <i>not</i> trigger entity scanning in the specified package; this is
	 * rather the job of {@link DefaultPersistenceUnitManager#setPackagesToScan}.
	 * @since 4.1
	 * @see SmartPersistenceUnitInfo#getManagedPackages()
	 * @see #addManagedClassName
	 */
	public void addManagedPackage(String packageName) {
		this.managedPackages.add(packageName);
	}

	@Override
	public List<String> getManagedPackages() {
		return this.managedPackages;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return this.excludeUnlistedClasses;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return this.sharedCacheMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	@Override
	public ValidationMode getValidationMode() {
		return this.validationMode;
	}

	public void addProperty(String name, String value) {
		this.properties.setProperty(name, value);
	}

	public void setProperties(Properties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return this.persistenceXMLSchemaVersion;
	}

	@Override
	public void setPersistenceProviderPackageName(@Nullable String persistenceProviderPackageName) {
		this.persistenceProviderPackageName = persistenceProviderPackageName;
	}

	public @Nullable String getPersistenceProviderPackageName() {
		return this.persistenceProviderPackageName;
	}


	/**
	 * This implementation returns the default ClassLoader.
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	@Override
	public @Nullable ClassLoader getClassLoader() {
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * This implementation throws an UnsupportedOperationException.
	 */
	@Override
	public void addTransformer(ClassTransformer classTransformer) {
		throw new UnsupportedOperationException("addTransformer not supported");
	}

	/**
	 * This implementation throws an UnsupportedOperationException.
	 */
	@Override
	public ClassLoader getNewTempClassLoader() {
		throw new UnsupportedOperationException("getNewTempClassLoader not supported");
	}


	@Override
	public String toString() {
		return "PersistenceUnitInfo: name '" + this.persistenceUnitName +
				"', root URL [" + this.persistenceUnitRootUrl + "]";
	}

}
