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
        // Las unidades y los identificadores son enteros: 50.9 no debe truncarse a 50.
        MAPPER.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);
        MAPPER.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(20)
                .maxNumberLength(64)
                .maxStringLength(AppConfig.maxRequestBytes())
                .build());
    }

    private JsonUtil() {
    }
}
