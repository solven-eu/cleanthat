package eu.solven.cleanthat.rules;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.rules.framework.AJavaParserRule;
import eu.solven.cleanthat.rules.framework.IJdkVersionConstants;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.meta.IRuleExternalUrls;

/**
 * Order modifiers according the the Java specification.
 *
 * @author Benoit Lacelle
 */
public class ModifierOrder extends AJavaParserRule implements IClassTransformer, IRuleExternalUrls {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModifierOrder.class);

	private static final List<String> ORDERED_MODIFIERS = ImmutableList.of("public",
			"protected",
			"private",
			"abstract",
			"default",
			"static",
			"final",
			"transient",
			"volatile",
			"synchronized",
			"native",
			"strictfp");

	@Override
	public String getId() {
		// Same name as checkstyle
		return "ModifierOrder";
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/modifier/ModifierOrderCheck.html";
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/reorder-modifiers.html";
	}

	@Override
	public boolean transformType(TypeDeclaration<?> tree) {
		AtomicBoolean transformed = new AtomicBoolean();

		tree.walk(node -> {
			if (node instanceof NodeWithModifiers<?>) {
				NodeWithModifiers<?> nodeWithModifiers = (NodeWithModifiers<?>) node;
				NodeList<Modifier> modifiers = nodeWithModifiers.getModifiers();

				NodeList<Modifier> mutableModifiers = new NodeList<>(modifiers);

				Collections.sort(mutableModifiers, new Comparator<Modifier>() {

					@Override
					public int compare(Modifier o1, Modifier o2) {
						return compare2(o1.getKeyword().asString(), o1.getKeyword().asString());
					}

					private int compare2(String left, String right) {
						return Integer.compare(ORDERED_MODIFIERS.indexOf(left), ORDERED_MODIFIERS.indexOf(right));
					}
				});

				boolean changed = false;
				for (int i = 0; i < modifiers.size(); i++) {
					// Check by reference
					if (modifiers.get(i) != mutableModifiers.get(i)) {
						changed = true;
						break;
					}
				}

				if (changed) {
					LOGGER.debug("We fixed the ordering of modifiers");
					nodeWithModifiers.setModifiers(mutableModifiers);
					transformed.set(true);
				}
			}

		});
		return transformed.get();
	}
}
