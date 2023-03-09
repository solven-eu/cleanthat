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
package eu.solven.cleanthat.engine.java.eclipse.cleanup;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.fix.ICleanUpCore;
import org.eclipse.jdt.internal.ui.fix.InvertEqualsCleanUpCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process code with standard Eclipse clean-up rules.
 * 
 * @author Benoit Lacelle
 *
 */
// https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Fguide%2Fjdt_api_contributing_a_cleanup.htm
// https://stackoverflow.com/questions/10120072/how-to-invoke-a-eclipse-clean-up-profile-programmatically
// https://stackoverflow.com/questions/11166862/eclipse-create-compilationunit-from-java-file
@Deprecated(since = "TODO")
public class EclipseCleanupProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseCleanupProcessor.class);

	public CleanUpContextCore makeContext(ICompilationUnit unit, CompilationUnit ast) {
		return new CleanUpContextCore(unit, ast);
	}

	public void simplifyRegex(Map<String, String> options,
			IJavaProject project,
			ICompilationUnit unit,
			IProgressMonitor monitor,
			CleanUpContextCore context) throws CoreException {
		ICleanUpCore cleanup = new InvertEqualsCleanUpCore(options);
		applyCleanup(project, unit, monitor, context, cleanup);
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

	public static void main(String[] args) {
		String sourceCode = "package eu.solven.cleanthat.do_not_format_me;\n" + "\n"
				+ "import java.time.LocalDate;\n"
				+ "import java.time.LocalDateTime;\n"
				+ "\n"
				+ "public class CleanClass {\n"
				+ "\n"
				+ "	final boolean b;\n"
				+ "\n"
				+ "	public CleanClass(String a, String b) {\n"
				+ "		this.b = a.equals(a + b);\n"
				+ "	}\n"
				+ "}\n";

		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(sourceCode.toCharArray());
		parser.setResolveBindings(true);
		CompilationUnit cUnit = (CompilationUnit) parser.createAST(null);

		LOGGER.info("{}", cUnit);
	}
}
