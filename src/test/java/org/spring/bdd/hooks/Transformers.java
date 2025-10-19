package org.spring.bdd.hooks;

import com.aventstack.extentreports.model.Author;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Transformers {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DefaultParameterTransformer
    @DefaultDataTableEntryTransformer
    @DefaultDataTableCellTransformer
    public Object transformer(Object fromValue, Type toValueType) {
        return objectMapper.convertValue(fromValue, objectMapper.constructType(toValueType));
    }

    @DocStringType
    public List<Map<String, String>> json(String docString) throws IOException {
        return objectMapper.readValue(docString, new TypeReference<List<Map<String, String>>>() {
        });
    }

    @DocStringType(contentType = "json")
    public Map<String, String> convertFoodItem(String docString) throws IOException {
        return objectMapper.readValue(docString, new TypeReference<Map<String, String>>() {
        });
    }

    @DataTableType
    public Author authorEntryTransformer(Map<String, String> entry) {
        return new Author(
                entry.get("firstName")
//                entry.get("lastName"),
//                entry.get("birthDate")
        );
    }
}