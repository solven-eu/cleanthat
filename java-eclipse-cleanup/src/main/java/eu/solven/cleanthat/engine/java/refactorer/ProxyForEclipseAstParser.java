package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ProxyForEclipseAstParser {

	private int jlsVersion;
	private String javaVersion;

	public ProxyForEclipseAstParser(int jlsVersion, String javaVersion) {
		this.jlsVersion = jlsVersion;
		this.javaVersion = javaVersion;
	}

	public CompilationUnit parseSourceCode(String sourceCode) {
		ASTParser parser = ASTParser.newParser(jlsVersion);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// parser.setSource(sourceCode.toCharArray());
		parser.setResolveBindings(true);

		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(javaVersion, options);
		parser.setCompilerOptions(options);

		return (CompilationUnit) parser.createAST(null);

		// see org.eclipse.jdt.core.dom.CompilationUnitResolver.parse(String[], String[], FileASTRequestor, int, Map,
		// int, IProgressMonitor)
		// see org.eclipse.jdt.core.dom.ASTParser.createASTs(String[], String[], String[], FileASTRequestor,
		// IProgressMonitor)
		// see org.eclipse.jdt.core.dom.ASTParser.createAST(IProgressMonitor)
		// javaParser.createASTs(new String[] {}, null, null, null, null);

		// org.eclipse.jdt.internal.compiler.batch.CompilationUnit compilationUnit =
		// new org.eclipse.jdt.internal.compiler.batch.CompilationUnit(sourceCode.toCharArray(),
		// "someFileName.java",
		// StandardCharsets.UTF_8.name());
		// org.eclipse.jdt.internal.compiler.env.ICompilationUnit sourceUnit = compilationUnit;
		// CompilationResult compilationResult =
		// new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
		// CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, compilationResult);
		//
		// if (compilationUnitDeclaration.ignoreMethodBodies) {
		// compilationUnitDeclaration.ignoreFurtherInvestigation = true;
		// // if initial diet parse did not work, no need to dig into method bodies.
		// continue;
		// }

		// fill the methods bodies in order for the code to be generated
		// real parse of the method....
		// org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
		// if (types != null) {
		// for (int j = 0, typeLength = types.length; j < typeLength; j++) {
		// types[j].parseMethods(parser, compilationUnitDeclaration);
		// }
		// }

		// convert AST
		// CompilationUnit node = CompilationUnitResolver.convert(compilationUnitDeclaration,
		// parser.scanner.getSource(),
		// apiLevel,
		// options,
		// false/* don't resolve binding */,
		// null/* no owner needed */,
		// null/* no binding table needed */,
		// flags /* flags */,
		// iterationMonitor,
		// true);
		// node.setTypeRoot(null);
	}

}
