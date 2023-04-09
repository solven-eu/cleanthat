package eu.solven.cleanthat.engine.java.refactorer.test;

import com.github.javaparser.ast.Node;

public interface IAstTestHelper<AST, R> {

	String astToString(AST ast);

	String resultToString(R result);

	AST convertToAst(Node pre);
}
