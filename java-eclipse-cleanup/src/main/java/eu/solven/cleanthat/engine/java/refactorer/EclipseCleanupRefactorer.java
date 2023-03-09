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

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;
import org.eclipse.jdt.internal.corext.fix.ICleanUpCore;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * A {@link IMutator} configuring over an Eclipse Cleanup {@link ICleanUpCore}
 * 
 * @author Benoit Lacelle
 *
 */
// https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Fguide%2Fjdt_api_manip.htm
@Deprecated(since = "Not functional at all")
public class EclipseCleanupRefactorer
		extends AAstRefactorer<CompilationUnit, ProxyForEclipseAstParser, CompilationUnit, EclipseCleanupMutator> {
	public EclipseCleanupRefactorer(List<EclipseCleanupMutator> mutators) {
		super(mutators);
	}

	@Override
	public String doFormat(String content) throws IOException {
		return applyTransformers(content);
	}

	@Override
	public String getId() {
		return "openrewrite";
	}

	@Override
	protected ProxyForEclipseAstParser makeAstParser() {
		return new ProxyForEclipseAstParser(AST.getJLSLatest(), JavaCore.VERSION_1_8);
	}

	@Override
	protected CompilationUnit parseSourceCode(ProxyForEclipseAstParser astParser, String sourceCode) {
		return astParser.parseSourceCode(sourceCode);
	}

	@Override
	protected String toString(CompilationUnit compilationUnit) {
		// see compilationUnit.toString();
		NaiveASTFlattener printer = new NaiveASTFlattener();
		compilationUnit.accept(printer);
		return printer.getResult();
	}

}
