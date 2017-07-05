package com.sean.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class JacksonSuport {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper defaultMapper = new ObjectMapper();

    static {

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        defaultMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        defaultMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        defaultMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        defaultMapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        defaultMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        defaultMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        defaultMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    }

    public static ValueNode parse(String json) {
        try {
            return mapper.readValue(json, ValueNode.class);
        } catch (Exception e) {
            String message = "json parse error :"
                    + (json == null ? "null" : json.substring(0, Math.min(100, json.length())));
            throw new RuntimeException(message, e);
        }
    }

    public static ValueNode parse(File file) {
        try {
            return mapper.readValue(file, ValueNode.class);
        } catch (Exception e) {
            String message = "file json parse error ";
            throw new RuntimeException(message, e);
        }
    }

    public static <T> T parseJson(String json, Class<T> type) {
        try {
            return (T) mapper.readValue(json, type);
        } catch (JsonParseException e) {
            throw new RuntimeException("Deserialize from JSON failed.", e);
        } catch (JsonMappingException e) {
            throw new RuntimeException("Deserialize from JSON failed.", e);
        } catch (IOException e) {
            throw new RuntimeException("Deserialize from JSON failed.", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T parseJsonToList(String json, TypeReference<T> typeReference) {
        if (json == null || "".equals(json.trim())) {
            return null;
        }
        try {
            return (T) mapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Deserialize from JSON failed.", e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json format error: " + obj, e);
        }
    }

    public static String toTrimjson(Object obj) {
        try {
            return defaultMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json format error: " + obj, e);
        }
    }

    public static void write(ValueNode node, JsonGenerator gen) throws IOException {
        switch (node.getNodeType()) {
            case BOOLEAN:
            gen.writeBoolean(node.asBoolean());
            break;
        case NULL:
            gen.writeNull();
            break;
            case NUMBER:
            writeNumber(node, gen);
            break;
            case STRING:
            gen.writeString(node.toString());
            break;
        }
    }

    private static void writeNumber(ValueNode node, JsonGenerator gen) throws IOException {
        JsonParser.NumberType num = node.numberType();
        switch (num){
            case INT:gen.writeNumber(node.asInt());break;
            case LONG:gen.writeNumber(node.asLong());break;
            case DOUBLE:gen.writeNumber(node.asDouble());break;
            case FLOAT:gen.writeNumber(node.asDouble());break;
        }
    }

    public static void main(String[] args) {
        TestBO tb = new TestBO();
        String result = JacksonSuport.toJson(tb);
        System.out.println(JacksonSuport.toJson(tb));
        TestBO tb2 = JacksonSuport.parseJson(result,TestBO.class);
        tb2.setName("json");
        System.out.println(JacksonSuport.toJson(tb2));
    }

    static  class TestBO{
        private String name;
        private int age;
        private List<String> nn = Lists.newArrayList("1","2");

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<String> getNn() {
            return nn;
        }

        public void setNn(List<String> nn) {
            this.nn = nn;
        }
    }
}
