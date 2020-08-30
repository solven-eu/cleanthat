package eu.solven.cleanthat.github;

import java.util.List;
import java.util.Map;

/**
 * The configuration of a formatting job
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanthatRepositoryProperties {

	private CleanthatMetaProperties meta;

	private List<Map<String, ?>> languages;

	// private CleanthatJavaProperties java;

	public CleanthatMetaProperties getMeta() {
		return meta;
	}

	public void setMeta(CleanthatMetaProperties meta) {
		this.meta = meta;
	}

	// @JsonProperty
	// public CleanthatJavaProperties getJava() {
	// return java;
	// }
	//
	// public void setJava(CleanthatJavaProperties java) {
	// this.java = java;
	// }

	public List<Map<String, ?>> getLanguages() {
		return languages;
	}

	public void setLanguages(List<Map<String, ?>> languages) {
		this.languages = languages;
	}
}
