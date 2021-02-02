package eu.solven.cleanthat.rules.cases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.solven.cleanthat.rules.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

//TODO Have a maven module per version of Java, to ensure the post is valid
public class UseDiamondOperatorJdk8Cases extends ACases {

	@Override
	public IClassTransformer getTransformer() {
		return new UseDiamondOperatorJdk8();
	}

	public static class CaseCollection implements ICaseOverMethod {
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
