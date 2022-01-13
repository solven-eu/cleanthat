package eu.solven.cleanthat.formatter;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.language.SourceCodeProperties;

public class TestSourceCodeFormatterHelper {
    @Test
    public void testMergeSourceCodeEol() {
        ObjectMapper om = ConfigHelpers.makeJsonObjectMapper();
        SourceCodeFormatterHelper helper = new SourceCodeFormatterHelper(om);

        SourceCodeProperties defaultP = new SourceCodeProperties();
        SourceCodeProperties windowsP = new SourceCodeProperties();
        windowsP.setLineEndingAsEnum(LineEnding.CRLF);

        Assert.assertEquals(LineEnding.UNKNOWN, defaultP.getLineEndingAsEnum());
        Assert.assertEquals(LineEnding.CRLF, windowsP.getLineEndingAsEnum());

        // windows as inner
        {
            Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class), om.convertValue(windowsP, Map.class));
            SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

            Assert.assertEquals(LineEnding.CRLF, merged.getLineEndingAsEnum());
        }

        // windows as outer
        {
            Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(windowsP, Map.class), om.convertValue(defaultP, Map.class));
            SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

            Assert.assertEquals(LineEnding.CRLF, merged.getLineEndingAsEnum());
        }

        // default and default
        {
            Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class), om.convertValue(defaultP, Map.class));
            SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

            Assert.assertEquals(LineEnding.UNKNOWN, merged.getLineEndingAsEnum());
        }
    }
}
