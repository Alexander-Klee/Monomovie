package de.amklee.monomovie.util;

import io.ktor.server.resources.RoutingKt;
import io.ktor.util.AttributeKey;

public class Internal {
    @SuppressWarnings("KotlinInternalInJava")
    public static AttributeKey<Object> getResourceInstanceKey() {
        return RoutingKt.getResourceInstanceKey();
    }
}
