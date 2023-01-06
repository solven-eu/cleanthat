package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.language.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.UseDiamondOperator;
import eu.solven.cleanthat.language.java.refactorer.test.ACases;

public class UseDiamondOperatorCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new UseDiamondOperator();
	}

	@CompareMethods
	public static class CaseCollection {
		public Map<String, List<String>> pre() {
			return new HashMap<String, List<String>>();
		}

		public Map<String, List<String>> post() {
			return new HashMap<>();
		}
	}

	@UnchangedMethod
	public static class CaseAnonymousClass {

		public Map<String, List<String>> post() {
			return new HashMap<String, List<String>>() {
				private static final long serialVersionUID = 1L;

				{
					this.put("k", List.of());
				}
			};
		}
	}

}
