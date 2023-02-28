package eu.solven.cleanthat.codeprovider;

import java.util.List;

/**
 * Default and simple implementation of {@link ICodeWritingMetadata}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeWritingMetadata implements ICodeWritingMetadata {
	final List<String> comments;
	final List<String> labels;

	public CodeWritingMetadata(List<String> comments, List<String> labels) {
		this.comments = comments;
		this.labels = labels;
	}

	@Override
	public List<String> getComments() {
		return comments;
	}

	public List<String> getLabels() {
		return labels;
	}

	public static ICodeWritingMetadata empty() {
		return new CodeWritingMetadata(List.of(), List.of());
	}

}
