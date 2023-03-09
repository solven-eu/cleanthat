/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.fix.ICleanUpCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;

/**
 * A {@link IMutator} configuring over an OpenRewrite {@link Recipe}
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated(since = "Not functional at all")
public class EclipseCleanupMutator implements IWalkingMutator<CompilationUnit, CompilationUnit> {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseCleanupMutator.class);

	final ICleanUpCore cleanup;

	public EclipseCleanupMutator(ICleanUpCore cleanup) {
		this.cleanup = cleanup;
	}

	@Override
	public Optional<CompilationUnit> walkAst(CompilationUnit pre) {
		try {
			applyCleanup(null, (ICompilationUnit) pre, null, null, cleanup);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

		return Optional.empty();
	}

	private void applyCleanup(IJavaProject project,
			ICompilationUnit unit,
			IProgressMonitor monitor,
			CleanUpContextCore context,
			ICleanUpCore cleanup) throws CoreException {
		RefactoringStatus preStatus = cleanup.checkPreConditions(project, new ICompilationUnit[] { unit }, monitor);
		LOGGER.info("pre status: {}", preStatus);

		CleanUpRequirementsCore requirements = cleanup.getRequirementsCore();
		LOGGER.info("requirements: {}", requirements);

		ICleanUpFixCore fixed = cleanup.createFixCore(context);
		CompilationUnitChange change = fixed.createChange(monitor);
		LOGGER.info("change: {}", change);

		RefactoringStatus postStatus = cleanup.checkPostConditions(monitor);
		LOGGER.info("post status: {}", postStatus);
	}

}
