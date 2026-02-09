import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.AnnotationIntrospector;

public class FindFilterIdExample {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();

        AnnotationIntrospector introspector =
                mapper.getSerializationConfig().getAnnotationIntrospector();

        AnnotatedClass annotatedClass =
                AnnotatedClass.resolve(
                        mapper.getSerializationConfig(),
                        User.class
                );

        Object filterId = introspector.findFilterId(annotatedClass);

        System.out.println(filterId); // prints: userFilter
    }
}
