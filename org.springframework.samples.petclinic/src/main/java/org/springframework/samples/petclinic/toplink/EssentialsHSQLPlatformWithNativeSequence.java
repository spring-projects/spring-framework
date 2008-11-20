package org.springframework.samples.petclinic.toplink;

import java.io.IOException;
import java.io.Writer;

import oracle.toplink.essentials.exceptions.ValidationException;
import oracle.toplink.essentials.platform.database.HSQLPlatform;
import oracle.toplink.essentials.queryframework.ValueReadQuery;

/**
 * Subclass of the TopLink Essentials default HSQLPlatform class, using native
 * HSQLDB identity columns for id generation.
 *
 * <p>Necessary for PetClinic's default data model, which relies on identity
 * columns: this is uniformly used across all persistence layer implementations
 * (JDBC, Hibernate, and JPA).
 *
 * @author Juergen Hoeller
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @since 1.2
 */
public class EssentialsHSQLPlatformWithNativeSequence extends HSQLPlatform {

	private static final long serialVersionUID = -55658009691346735L;


	public EssentialsHSQLPlatformWithNativeSequence() {
		// setUsesNativeSequencing(true);
	}

	@Override
	public boolean supportsNativeSequenceNumbers() {
		return true;
	}

	@Override
	public boolean shouldNativeSequenceAcquireValueAfterInsert() {
		return true;
	}

	@Override
	public ValueReadQuery buildSelectQueryForNativeSequence() {
		return new ValueReadQuery("CALL IDENTITY()");
	}

	@Override
	public void printFieldIdentityClause(Writer writer) throws ValidationException {
		try {
			writer.write(" IDENTITY");
		}
		catch (IOException ex) {
			throw ValidationException.fileError(ex);
		}
	}

}
