package eu.solven.cleanthat.language.java.imports;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Configuration for Java Revelc imports cleaner
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class JavaRevelcImportsCleanerProperties {

    private static final boolean DEFAULT_REMOVED_UNUSED = true;
    private static final String DEFAULT_ECLIPSE_GROUPS = "java.,javax.,org.,com.";
    private static final String DEFAULT_ECLIPSE_STATIC_GROUPS = "java,*";
    private static final boolean DEFAULT_STATIC_AFTER = false;

    // https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#removeUnused
    private boolean removeUnused = DEFAULT_REMOVED_UNUSED;

    // https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#groups
    private String groups = DEFAULT_ECLIPSE_GROUPS;

    // https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#staticGroups
    private String staticGroups = DEFAULT_ECLIPSE_STATIC_GROUPS;

    // https://code.revelc.net/impsort-maven-plugin/sort-mojo.html#staticAfter
    private boolean staticAfter = DEFAULT_STATIC_AFTER;

    public JavaRevelcImportsCleanerProperties eclipse() {
        // Constructor applies eclipse as default
        return new JavaRevelcImportsCleanerProperties();
    }

    public JavaRevelcImportsCleanerProperties intellij() {
        JavaRevelcImportsCleanerProperties p = new JavaRevelcImportsCleanerProperties();

        // https://github.com/checkstyle/checkstyle/issues/5510
        // https://github.com/revelc/impsort-maven-plugin/issues/28
        p.setGroups("*,javax.,java.");
        p.setStaticGroups("*");
        p.setStaticAfter(true);
        p.setRemoveUnused(true);

        return p;
    }
}
