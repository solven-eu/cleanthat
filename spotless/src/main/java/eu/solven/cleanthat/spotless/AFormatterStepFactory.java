package eu.solven.cleanthat.spotless;

import static java.util.Collections.emptySet;

import java.util.Set;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.google.common.collect.Sets;

import eu.solven.cleanthat.codeprovider.ICodeProvider;

// see com.diffplug.spotless.maven.FormatterFactory
public abstract class AFormatterStepFactory {

	private String[] includes;

	private String[] excludes;

	// private final List<FormatterStepFactory> stepFactories = new ArrayList<>();

	// private ToggleOffOn toggle;

	final ICodeProvider codeProvider;

	public AFormatterStepFactory(ICodeProvider codeProvider, String[] includes, String[] excludes) {
		this.codeProvider = codeProvider;
		this.includes = includes;
		this.excludes = excludes;
	}

	public abstract Set<String> defaultIncludes();

	public abstract String licenseHeaderDelimiter();

	public final Set<String> includes() {
		return includes == null ? emptySet() : Sets.newHashSet(includes);
	}

	public final Set<String> excludes() {
		return excludes == null ? emptySet() : Sets.newHashSet(excludes);
	}

	public abstract FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner);

}
