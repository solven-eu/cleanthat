package eu.solven.cleanthat.rules.cases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.solven.cleanthat.language.java.rules.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.cases.annotations.CompareMethods;
import eu.solven.cleanthat.rules.test.ACases;

//TODO Have a maven module per version of Java, to ensure the post is valid
public class UseDiamondOperatorJdk8Cases extends ACases {

	@Override
	public IClassTransformer getTransformer() {
		return new UseDiamondOperatorJdk8();
	}

	@CompareMethods
	public static class CaseCollection {
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
