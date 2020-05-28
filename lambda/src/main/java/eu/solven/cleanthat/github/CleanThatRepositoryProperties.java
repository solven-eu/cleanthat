package eu.solven.cleanthat.github;

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

}
