package de.swiftbird.elasticandroid;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.net.InetAddress;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class AppConverters {
    @TypeConverter
    public static List<InetAddress> fromString(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<InetAddress>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromList(List<InetAddress> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    // For string lists

    @TypeConverter
    public static List<String> fromStringList(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromStringList(List<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static String fromGeoLocation(LocationCompDocument.GeoLocation geoLocation) {
        if (geoLocation == null) return null;
        return geoLocation.lat + "," + geoLocation.lon;
    }

    @TypeConverter
    public static LocationCompDocument.GeoLocation toGeoLocation(String data) {
        if (data == null) return null;
        String[] pieces = data.split(",");
        return new LocationCompDocument.GeoLocation(Double.parseDouble(pieces[0]), Double.parseDouble(pieces[1]));
    }
}
