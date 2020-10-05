package eu.solven.cleanthat.java.imports;

import java.nio.charset.StandardCharsets;

/**
 * Configuration for Java Revelc imports cleaner
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaRevelcImportsCleanerProperties {

	String encoding = StandardCharsets.UTF_8.name();

	boolean removeUnusedImports = true;

	String groups = "java.,javax.,org.,com.";
	String staticGroups = "java,*";

	public String getGroups() {
		return groups;
	}

	public String getStaticGroups() {
		return staticGroups;
	}

	public String getEncoding() {
		return encoding;
	}

	public boolean isRemoveUnusedImports() {
		return removeUnusedImports;
	}

}
