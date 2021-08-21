package eu.solven.cleanthat.language.java.rules.test;

import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;

public abstract class ACases {
	public String getId() {
		return getTransformer().getClass().getName();
	}

	public abstract IClassTransformer getTransformer();

}
