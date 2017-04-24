package com.sean.json;


import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;

/**
 * Created by guozhenbin on 2017/4/6.
 */
public class JsonMapper {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        //设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.ALWAYS);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //允许字段名不带引号
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // allow JSON Strings to contain unquoted control characters
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,true);
        String jsonString = "<!>";
        Object t =  mapper.readValue(jsonString, JsonMapper.class);
        System.out.println(t);
    }

}
