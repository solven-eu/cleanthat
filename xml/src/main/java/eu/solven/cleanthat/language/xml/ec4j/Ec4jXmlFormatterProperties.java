package eu.solven.cleanthat.language.xml.ec4j;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.language.SourceCodeProperties;

/**
 * Configuration for EC4J Xml formatter
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Ec4jXmlFormatterProperties {

    private static final int DEFAULT_INDENT_WHITESPACES = 4;

    int indent = DEFAULT_INDENT_WHITESPACES;

    boolean spaceBeforeSeparator = true;
    boolean alphabeticalOrder = false;
    boolean eolAtEof = false;

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }

    public boolean isSpaceBeforeSeparator() {
        return spaceBeforeSeparator;
    }

    public void setSpaceBeforeSeparator(boolean spaceBeforeSeparator) {
        this.spaceBeforeSeparator = spaceBeforeSeparator;
    }

    public boolean isAlphabeticalOrder() {
        return alphabeticalOrder;
    }

    public void setAlphabeticalOrder(boolean alphabeticalOrder) {
        this.alphabeticalOrder = alphabeticalOrder;
    }

    public boolean isEolAtEof() {
        return eolAtEof;
    }

    public void setEolAtEof(boolean eolAtEof) {
        this.eolAtEof = eolAtEof;
    }
}
