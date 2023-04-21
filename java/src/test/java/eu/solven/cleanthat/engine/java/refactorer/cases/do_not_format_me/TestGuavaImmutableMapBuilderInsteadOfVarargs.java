package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.Ignore;

import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaImmutableMapBuilderOverVarargs;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestGuavaImmutableMapBuilderInsteadOfVarargs extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new GuavaImmutableMapBuilderOverVarargs();
	}

	@UnmodifiedMethod
	public static class Empty {
		public Object pre() {
			return ImmutableMap.of();
		}
	}

	@UnmodifiedMethod
	public static class SingleEntry {
		public Object pre() {
			return ImmutableMap.of("k0", 0D);
		}
	}

	@CompareMethods
	public static class TwoEntry {
		public Object pre() {
			return ImmutableMap.of("k0", 0, "k1", 1D);
		}

		public Object post() {
			return ImmutableMap.builder().put("k0", 0).put("k1", 1D).build();
		}

		public Object post_alternative() {
			return ImmutableMap.<java.lang.String, java.lang.Number>builder().put("k0", 0).put("k1", 1D).build();
		}
	}

	@CompareMethods
	public static class ThreeEntry {
		public Object pre() {
			return ImmutableMap.of("k0", 0, "k1", 1D, "k2", 2L);
		}

		public Object post() {
			return ImmutableMap.builder().put("k0", 0).put("k1", 1D).put("k2", 2L).build();
		}

		public Object post_alternative() {
			return ImmutableMap.<java.lang.String, java.lang.Object>builder()
					.put("k0", 0)
					.put("k1", 1D)
					.put("k2", 2L)
					.build();
		}
	}

	@UnmodifiedMethod
	public static class JavaUtilMap {
		public Object pre() {
			return Map.of("k0", 0, "k1", 1D, "k2", 2L);
		}
	}

	@Ignore("TODO")
	@CompareMethods
	public static class WithGenerics_Wildcard {
		public Map<String, ?> pre() {
			return ImmutableMap.of("k0", 0, "k1", 1D);
		}

		public Map<String, ?> post() {
			return ImmutableMap.<String, Object>builder().put("k0", 0).put("k1", 1D).build();
		}
	}

	@Ignore("TODO")
	@CompareMethods
	public static class WithGenerics_Wildcard_inLambda {
		public Map<Number, ?> pre() {
			Supplier<Map<String, ?>> mapSupplier = () -> {
				return ImmutableMap.of("k0", 0, "k1", 1D);
			};
			return Map.of(0, mapSupplier);
		}

		public Map<Number, ?> post() {
			Supplier<Map<String, ?>> mapSupplier = () -> {
				return ImmutableMap.<String, Object>builder().put("k0", 0).put("k1", 1).build();
			};
			return Map.of(0, mapSupplier);
		}
	}
}
