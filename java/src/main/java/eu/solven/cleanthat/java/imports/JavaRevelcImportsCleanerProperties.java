package eu.solven.cleanthat.java.imports;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Configuration for Java Revelc imports cleaner
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class JavaRevelcImportsCleanerProperties {

	// https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#removeUnused
	boolean removeUnused = true;

	// https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#groups
	String groups = "java.,javax.,org.,com.";

	// https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#staticGroups
	String staticGroups = "java,*";

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getStaticGroups() {
		return staticGroups;
	}

	public void setStaticGroups(String staticGroups) {
		this.staticGroups = staticGroups;
	}

	public boolean isRemoveUnused() {
		return removeUnused;
	}

	public void setRemoveUnusedImports(boolean removeUnusedImports) {
		this.removeUnused = removeUnusedImports;
	}
}
