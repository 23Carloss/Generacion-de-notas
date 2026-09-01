package Util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.StreamReadConstraints;

/**
 * ObjectMapper compartido por todos los servlets. Rechaza propiedades no
 * esperadas para limitar mass assignment y errores de integración.
 */
public class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        MAPPER.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(20)
                .maxNumberLength(64)
                .maxStringLength(AppConfig.maxRequestBytes())
                .build());
    }

    private JsonUtil() {
    }
}
