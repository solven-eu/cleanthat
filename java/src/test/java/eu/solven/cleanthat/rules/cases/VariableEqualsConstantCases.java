package eu.solven.cleanthat.rules.cases;

import org.junit.Ignore;

import eu.solven.cleanthat.rules.VariableEqualsConstant;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverClass;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

public class VariableEqualsConstantCases extends ACases {

	public IClassTransformer getTransformer() {
		return new VariableEqualsConstant();
	}

	public static class CaseConstantString implements ICaseOverMethod {
		public Object pre(String input) {
			return input.equals("hardcoded");
		}

		public Object post(String input) {
			return "hardcoded".equals(input);
		}
	}

	// https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#literalsfirstincomparisons
	@Ignore("Not ready")
	public static class CasePMD implements ICaseOverClass {

		class Pre {
			boolean stringEquals(String x) {
				return x.equals("2"); // should be "2".equals(x)
			}

			boolean stringEqualsIgnoreCase(String x) {
				return x.equalsIgnoreCase("2"); // should be "2".equalsIgnoreCase(x)
			}

			boolean stringCompareTo(String x) {
				return (x.compareTo("bar") > 0); // should be: "bar".compareTo(x) < 0
			}

			boolean stringCompareToIgnoreCase(String x) {
				return (x.compareToIgnoreCase("bar") > 0); // should be: "bar".compareToIgnoreCase(x) < 0
			}

			boolean stringContentEquals(String x) {
				return x.contentEquals("bar"); // should be "bar".contentEquals(x)
			}
		}

		class Post {
			boolean stringEquals(String x) {
				return "2".equals(x);
			}

			boolean stringEqualsIgnoreCase(String x) {
				return "2".equalsIgnoreCase(x);
			}

			boolean stringCompareTo(String x) {
				return ("bar".compareTo(x) < 0);
			}

			boolean stringCompareToIgnoreCase(String x) {
				return ("bar".compareToIgnoreCase(x) < 0);
			}

			boolean stringContentEquals(String x) {
				return "bar".contentEquals(x);
			}
		}

	}

}
