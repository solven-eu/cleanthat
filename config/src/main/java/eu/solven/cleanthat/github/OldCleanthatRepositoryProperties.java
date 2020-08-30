package eu.solven.cleanthat.github;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import eu.solven.cleanthat.formatter.LineEnding;

/**
 * The configuration of a formatting job
 * 
 * @author Benoit Lacelle
 *
 */
public class OldCleanthatRepositoryProperties {
	// Either we commit directly in PR, or we open new PR to merge in existing ones
	boolean appendToExistingPullRequest = false;

	// May be
	// https://raw.githubusercontent.com/solven-eu/pepper/master/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml
	String javaConfigUrl = null;

	String javaEngine = "eclipse";

	LineEnding eol = LineEnding.valueOf("LF");

	// If empty, no file is exclude
	List<String> excludes = Arrays.asList();

	// If empty, no file is included
	List<String> includes = Arrays.asList("regex:.*\\.java");

	String encoding = StandardCharsets.UTF_8.name();

	boolean removeUnusedImports = true;

	String groups = "java.,javax.,org.,com.";
	String staticGroups = "java,*";

	public boolean isAppendToExistingPullRequest() {
		return appendToExistingPullRequest;
	}

	public void setAppendToExistingPullRequest(boolean appendToExistingPullRequest) {
		this.appendToExistingPullRequest = appendToExistingPullRequest;
	}

	public String getJavaConfigUrl() {
		return javaConfigUrl;
	}

	public void setJavaConfigUrl(String javaConfigUrl) {
		this.javaConfigUrl = javaConfigUrl;
	}

	public LineEnding getLineEnding() {
		return this.eol;
	}

	public void setEol(String eol) {
		this.eol = LineEnding.valueOf(eol);
	}

	public String getJavaEngine() {
		return javaEngine;
	}

	public List<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public boolean isRemoveUnusedImports() {
		return removeUnusedImports;
	}

	public void setRemoveUnusedImports(boolean removeUnusedImports) {
		this.removeUnusedImports = removeUnusedImports;
	}

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
}
