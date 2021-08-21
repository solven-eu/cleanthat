package eu.solven.cleanthat.java.mutators;

// Javaparser will remove some of the consecutive EOL in these comments
/**
 *
 *
 * This class holds various examples of code which is dirty by a Javaparser clean parsing/toString
 * 
 *
 * @author Benoit Lacelle
 *
 *
 */
public class JavaparserDirtyMe {

	// JavaParser will remove the EOL between these lines
	public String someMethodWithEol() {
		String string = "a";
		string += "b";

		string += "c";
		string += "d";

		return string;
	}
}
