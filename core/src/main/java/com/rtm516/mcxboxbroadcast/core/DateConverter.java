package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * A Gson adapter for converting {@link Date} objects to and from the variations of ISO 8601 format used by Xbox Live.
 *
 * They can't decide on what format to use so we have to support all of them.
 * yyyy-MM-dd'T'HH:mm:ss.SSSSSS
 * yyyy-MM-dd'T'HH:mm:ss.SSSSSSS
 * yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'
 */
public class DateConverter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private static final DateTimeFormatter dateFormat;

    static {
        dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS").withZone(ZoneOffset.UTC);
    }

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            String dateString = json.getAsString();

            // Remove the Z from the end of the string
            if (dateString.endsWith("Z")) {
                dateString = dateString.substring(0, dateString.length() - 1);
            }

            // Add missing fractional digits
            if (dateString.contains(".")) {
                int fractionalDigits = dateString.length() - dateString.indexOf('.') - 1;
                if (fractionalDigits < 7) {
                    dateString += "0".repeat(7 - fractionalDigits);
                }
            } else if (!dateString.isBlank()) {
                dateString += ".0000000"; // Add fractional part if missing
            }

            Instant instant = dateFormat.parse(dateString, Instant::from);
            return Date.from(instant);
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(dateFormat.format(src.toInstant()));
    }
}
