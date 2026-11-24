package com.sz.admin.shop_system.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Iterator;

/**
 * 兼容前端传参为字符串、对象、数组时，统一提取图片 URL。
 */
public class StringOrArrayToStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return extractString(node);
    }

    private String extractString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                String value = extractString(iterator.next());
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            }
            return null;
        }
        if (node.isObject()) {
            JsonNode urlNode = node.get("url");
            if (urlNode != null && urlNode.isTextual()) {
                return urlNode.asText();
            }
            JsonNode objectNameNode = node.get("objectName");
            if (objectNameNode != null && objectNameNode.isTextual()) {
                return objectNameNode.asText();
            }
            return null;
        }
        return node.asText();
    }
}
