package de.swiftbird.elasticandroid;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.net.InetAddress;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * AppConverters provides a collection of methods to assist Room in converting complex data types to and from
 * a format that can be stored in the database easily. This includes conversions for lists of InetAddress objects,
 * strings, and custom objects like GeoLocation, facilitating their storage in a single column.
 */
public class AppConverters {

    /**
     * Converts a JSON string representation of a list of InetAddress objects back into a List.
     * @param value JSON string representing the list of InetAddress objects.
     * @return A List of InetAddress objects or an empty list if the input is null.
     */
    @TypeConverter
    public static List<InetAddress> fromString(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<InetAddress>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    /**
     * Converts a List of InetAddress objects into a JSON string representation.
     * @param list The List of InetAddress objects to convert.
     * @return A JSON string representation of the list.
     */
    @TypeConverter
    public static String fromList(List<InetAddress> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }



    /**
     * Converts a JSON string representation of a list of strings back into a List.
     * @param value JSON string representing the list of strings.
     * @return A List of strings or an empty list if the input is null.
     */
    @TypeConverter
    public static List<String> fromStringList(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    /**
     * Converts a List of strings into a JSON string representation.
     * @param list The List of strings to convert.
     * @return A JSON string representation of the list.
     */
    @TypeConverter
    public static String fromStringList(List<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    /**
     * Converts a GeoLocation object into a simplified string representation "lat,lon".
     * @param geoLocation The GeoLocation object to convert.
     * @return A string representing the latitude and longitude, separated by a comma.
     */
    @TypeConverter
    public static String fromGeoLocation(LocationCompDocument.GeoLocation geoLocation) {
        if (geoLocation == null) return null;
        return geoLocation.lat + "," + geoLocation.lon;
    }

    /**
     * Converts a simplified string representation "lat,lon" of a GeoLocation back into a GeoLocation object.
     * @param data The string representation of the GeoLocation.
     * @return A GeoLocation object constructed from the provided string.
     */
    @TypeConverter
    public static LocationCompDocument.GeoLocation toGeoLocation(String data) {
        if (data == null) return null;
        String[] pieces = data.split(",");
        return new LocationCompDocument.GeoLocation(Double.parseDouble(pieces[0]), Double.parseDouble(pieces[1]));
    }
}
