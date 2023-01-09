package eu.solven.cleanthat.language.java.spotless;

import static java.util.Collections.emptySet;

import java.util.Set;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.google.common.collect.Sets;

// see com.diffplug.spotless.maven.FormatterFactory
public abstract class AFormatterStepFactory {

	private String[] includes;

	private String[] excludes;

	// private final List<FormatterStepFactory> stepFactories = new ArrayList<>();

	// private ToggleOffOn toggle;

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
