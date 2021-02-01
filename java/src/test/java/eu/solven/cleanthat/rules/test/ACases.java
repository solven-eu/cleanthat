package eu.solven.cleanthat.rules.test;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

public abstract class ACases {
	public String getId() {
		return getTransformer().getClass().getName();
	}

	public abstract IClassTransformer getTransformer();

}
