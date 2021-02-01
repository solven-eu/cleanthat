package eu.solven.cleanthat.rules.cases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.solven.cleanthat.rules.UseDiamondOperator;
import eu.solven.cleanthat.rules.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

//TODO Have a maven module per version of Java, to ensure the post is valid
public class UseDiamondOperatorJdk8Cases {
	public String getId() {
		return UseDiamondOperator.class.getSimpleName();
	}

	public IClassTransformer getTransformer() {
		return new UseDiamondOperatorJdk8();
	}

	public static class CaseCollection {
		public String getTitle() {
			return "TypeArgumentOnInvocationParameter";
		}

		private <T> List<T> genericMethod(List<T> list) {
			return list;
		}

		public Object pre() {
			return genericMethod(new ArrayList<Number>());
		}

		public Object post(Collection<?> input) {
			return genericMethod(new ArrayList<>());
		}
	}
}
