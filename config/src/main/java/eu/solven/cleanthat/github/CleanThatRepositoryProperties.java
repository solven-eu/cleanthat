package eu.solven.cleanthat.github;

import io.cormoran.cleanthat.formatter.LineEnding;

/**
 * The configuration of a formatting job
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanThatRepositoryProperties {
	// Either we commit directly in PR, or we open new PR to merge in existing ones
	boolean appendToExistingPullRequest = false;

	// May be
	// https://raw.githubusercontent.com/solven-eu/pepper/master/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml
	String javaConfigUrl = null;

	String javaEngine = "eclipse";

	// Git has some preference to committing LF
	// https://code.revelc.net/formatter-maven-plugin/format-mojo.html#lineEnding
	LineEnding eol = LineEnding.valueOf("LF");

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

}
