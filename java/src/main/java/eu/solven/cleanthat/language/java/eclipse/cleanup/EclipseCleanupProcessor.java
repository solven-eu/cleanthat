package eu.solven.cleanthat.language.java.eclipse.cleanup;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.ui.fix.PlainReplacementCleanUpCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process code with standard Eclipse clean-up rules.
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated(since = "TODO")
public class EclipseCleanupProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseCleanupProcessor.class);

	public CleanUpContextCore makeContext(ICompilationUnit unit, CompilationUnit ast) {
		return new CleanUpContextCore(unit, ast);
	}

	public void simplifyRegex(Map<String, String> options,
			IJavaProject project,
			ICompilationUnit[] units,
			IProgressMonitor monitor,
			CleanUpContextCore context) throws CoreException {
		PlainReplacementCleanUpCore cleanup = new PlainReplacementCleanUpCore(options);
		RefactoringStatus preStatus = cleanup.checkPreConditions(project, units, monitor);
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
