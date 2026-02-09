package example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class JacksonJavaTypeExample {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Example JSON: a list of maps (List<Map<String, Object>>)
        String json = """
                [
                  { "name": "Alice", "age": 30, "active": true },
                  { "name": "Bob", "age": 25, "active": false }
                ]
                """;

        // Build a JavaType for List<Map<String, Object>> using com.fasterxml.jackson.databind.JavaType
        JavaType mapType = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);

        JavaType listOfMapsType = mapper.getTypeFactory()
                .constructCollectionType(List.class, mapType);

        // Deserialize using the constructed JavaType
        List<Map<String, Object>> data = mapper.readValue(json, listOfMapsType);

        System.out.println("First name: " + data.get(0).get("name"));
        System.out.println("Second active: " + data.get(1).get("active"));

        // Optional: serialize back to JSON
        String roundTrip = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        System.out.println(roundTrip);

        // Alternative example: JavaType from a TypeReference (still yields a JavaType internally)
        JavaType fromTypeRef = mapper.getTypeFactory()
                .constructType(new TypeReference<List<Map<String, Object>>>() {});
        List<Map<String, Object>> data2 = mapper.readValue(json, fromTypeRef);

        System.out.println("Parsed again, size: " + data2.size());
    }
}
