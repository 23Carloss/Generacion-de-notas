package Util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ObjectMapper compartido por todos los servlets. Se configura una sola vez
 * para no fallar si el JSON de entrada trae propiedades que el DTO no tiene.
 */
public class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtil() {
    }
}
