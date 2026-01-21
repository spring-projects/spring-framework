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

import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Spring's mutable equivalent of the JPA
 * {@link jakarta.persistence.spi.PersistenceUnitInfo} interface,
 * used to bootstrap an {@code EntityManagerFactory} in a container.
 * This is the type exposed to {@link PersistenceUnitPostProcessor}.
 *
 * <p>This implementation is largely a JavaBean, offering mutators
 * for all standard {@code PersistenceUnitInfo} properties.
 * As of 7.0, it does <i>not</i> implement {@code PersistenceUnitInfo} but
 * rather serves as the state behind a runtime {@code PersistenceUnitInfo}
 * (for achieving compatibility between JPA 3.2 and 4.0 and for preventing
 * late mutation attempts through {@code PersistenceUnitInfo} downcasts).
 *
 * <p>For custom bootstrapping purposes, use {@link SpringPersistenceUnitInfo}
 * instead, turning it into a {@code jakarta.persistence.spi.PersistenceUnitInfo}
 * through {@link SpringPersistenceUnitInfo#asStandardPersistenceUnitInfo()}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 * @see PersistenceUnitPostProcessor#postProcessPersistenceUnitInfo
 * @see SpringPersistenceUnitInfo#asStandardPersistenceUnitInfo()
 */
public class MutablePersistenceUnitInfo {

	private @Nullable String persistenceUnitName;

	private @Nullable String persistenceProviderClassName;

	private @Nullable PersistenceUnitTransactionType transactionType;

	private @Nullable DataSource jtaDataSource;

	private @Nullable DataSource nonJtaDataSource;

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

	public @Nullable String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	public void setPersistenceProviderClassName(@Nullable String persistenceProviderClassName) {
		this.persistenceProviderClassName = persistenceProviderClassName;
	}

	public @Nullable String getPersistenceProviderClassName() {
		return this.persistenceProviderClassName;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

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

	public @Nullable DataSource getJtaDataSource() {
		return this.jtaDataSource;
	}

	public void setNonJtaDataSource(@Nullable DataSource nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	public @Nullable DataSource getNonJtaDataSource() {
		return this.nonJtaDataSource;
	}

	public void addMappingFileName(String mappingFileName) {
		this.mappingFileNames.add(mappingFileName);
	}

	public List<String> getMappingFileNames() {
		return this.mappingFileNames;
	}

	public void addJarFileUrl(URL jarFileUrl) {
		this.jarFileUrls.add(jarFileUrl);
	}

	public List<URL> getJarFileUrls() {
		return this.jarFileUrls;
	}

	public void setPersistenceUnitRootUrl(@Nullable URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

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

	public List<String> getManagedPackages() {
		return this.managedPackages;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	public boolean excludeUnlistedClasses() {
		return this.excludeUnlistedClasses;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	public SharedCacheMode getSharedCacheMode() {
		return this.sharedCacheMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

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

	public Properties getProperties() {
		return this.properties;
	}

	public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
	}

	public String getPersistenceXMLSchemaVersion() {
		return this.persistenceXMLSchemaVersion;
	}

	public void setPersistenceProviderPackageName(@Nullable String persistenceProviderPackageName) {
		this.persistenceProviderPackageName = persistenceProviderPackageName;
	}

	public @Nullable String getPersistenceProviderPackageName() {
		return this.persistenceProviderPackageName;
	}


	@Override
	public String toString() {
		return "PersistenceUnitInfo: name '" + this.persistenceUnitName +
				"', root URL [" + this.persistenceUnitRootUrl + "]";
	}

}
