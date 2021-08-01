package eu.solven.cleanthat.aws.dynamodb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.jackson.CustomSnakeCase;

public class TestParseDynamoDbJson {
	final ObjectMapper om = AWebhooksLambdaFunction.configureForDynamoDb(new ObjectMapper());

	@Test
	public void testReadWriteJson() throws JsonMappingException, JsonProcessingException {
		Map<String, Object> originalMap = new LinkedHashMap<>();

		originalMap.put("k", "v");
		originalMap.put("k2", Map.of("k22", "vv"));
		originalMap.put("k3", Map.of("k33", Map.of("k333", "vvv")));

		Map<String, AttributeValue> dynamoDbFormat = ItemUtils.fromSimpleMap(originalMap);

		{
			Assert.assertEquals(new AttributeValue().withS("v"), dynamoDbFormat.get("k"));
		}

		Map<String, ?> backToMap = InternalUtils.toSimpleMapValue(dynamoDbFormat);
		{
			Assert.assertEquals(originalMap, backToMap);
		}

		String dynamoDbFormatAsJson = om.writeValueAsString(dynamoDbFormat);
		Map<String, ?> dynamoDbPureJson = om.readValue(dynamoDbFormatAsJson, Map.class);

		{
			Assert.assertEquals(Map.of("S", "v"), dynamoDbPureJson.get("k"));
		}

		Map<String, AttributeValue> dynamoDbAsAttributeValue =
				om.readValue(dynamoDbFormatAsJson, new TypeReference<Map<String, AttributeValue>>() {

				});
		{
			Assert.assertEquals(dynamoDbFormat, dynamoDbAsAttributeValue);
		}

		Map<String, AttributeValue> dynamoDbAsAttributeValueFromJsonAsMap =
				om.convertValue(dynamoDbAsAttributeValue, new TypeReference<Map<String, AttributeValue>>() {

				});
		{
			Assert.assertEquals(dynamoDbFormat, dynamoDbAsAttributeValueFromJsonAsMap);
		}
	}
}
