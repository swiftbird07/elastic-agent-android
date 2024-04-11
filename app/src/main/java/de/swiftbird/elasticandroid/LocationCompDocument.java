package de.swiftbird.elasticandroid;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Represents location events captured by the application, including GPS coordinates and geographical information.
 * This entity is stored in the application's Room database and is used to aggregate location events before processing
 * or transmission to a centralized logging system.
 * <p>
*  The class extends {@link ElasticDocument} to include common Elastic Stack fields and additional location-specific
*  information such as geographical names, postal codes, and street addresses.
*  Fields are annotated for serialization into JSON using {@link com.google.gson.annotations.SerializedName}
*  and for storage in the SQLite database using Room annotations.
*  <p>
*  The class also includes a nested class {@link GeoLocation} to represent a geographical location with latitude
*  and longitude.
*  </p>
 */
@Entity
public class LocationCompDocument extends ElasticDocument {
    // Event action and category for location updates
    private static final String EVENT_ACTION = "location-update";
    private static final String EVENT_CATEGORY = "location";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    // ECS fields
    @SerializedName("event.action")
    @ColumnInfo(name = "eventAction")
    public String eventAction;

    @SerializedName("event.category")
    @ColumnInfo(name = "eventCategory")
    public String eventCategory;

    @SerializedName("observer.geo.location")
    public GeoLocation observerGeoLocation;

    /**
     * Nested class to represent an ECS geo-location with latitude and longitude.
     */
    public static class GeoLocation {
        @SerializedName("lat")
        public double lat;

        @SerializedName("lon")
        public double lon;

        public GeoLocation(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    @SerializedName("observer.geo.city_name")
    @ColumnInfo(name = "observerGeoCityName")
    public String observerGeoCityName;

    @SerializedName("observer.geo.continent_code")
    @ColumnInfo(name = "observerGeoContinentCode")
    public String observerGeoContinentCode;

    @SerializedName("observer.geo.continent_name")
    @ColumnInfo(name = "observerGeoContinentName")
    public String observerGeoContinentName;

    @SerializedName("observer.geo.country_iso_code")
    @ColumnInfo(name = "observerGeoCountryIsoCode")
    public String observerGeoCountryIsoCode;

    @SerializedName("observer.geo.country_name")
    @ColumnInfo(name = "observerGeoCountryName")
    public String observerGeoCountryName;

    @SerializedName("observer.geo.name")
    @ColumnInfo(name = "observerGeoName")
    public String observerGeoName;

    @SerializedName("observer.geo.postal_code")
    @ColumnInfo(name = "observerGeoPostalCode")
    public String observerGeoPostalCode;

    @SerializedName("observer.geo.region_iso_code")
    @ColumnInfo(name = "observerGeoRegionIsoCode")
    public String observerGeoRegionIsoCode;

    @SerializedName("observer.geo.region_name")
    @ColumnInfo(name = "observerGeoRegionName")
    public String observerGeoRegionName;

    // Additional non-ECS field that is parsed by GeoCoder but not part of ECS

    @SerializedName("observer.geo.street_address")
    @ColumnInfo(name = "observerGeoStreetAddress")
    public String observerGeoStreetAddress;


    // Some other non-ECS fields that are provided directly by the location object

    @SerializedName("location.provider")
    @ColumnInfo(name = "locationProvider")
    public String locationProvider;

    @SerializedName("location.accuracy")
    @ColumnInfo(name = "locationAccuracy")
    public float locationAccuracy;

    @SerializedName("location.altitude")
    @ColumnInfo(name = "locationAltitude")
    public double locationAltitude;

    @SerializedName("location.speed")
    @ColumnInfo(name = "locationSpeed")
    public float locationSpeed;

    @SerializedName("location.bearing")
    @ColumnInfo(name = "locationBearing")
    public float locationBearing;

    @SerializedName("location.time")
    @ColumnInfo(name = "locationTime")
    public long locationTime;


    public LocationCompDocument() {
    }

    /**
     * Constructs a LocationCompDocument from a Location object, enriching it with geographical information using the Geocoder if possible.
     *
     * @param location The raw location data.
     * @param enrollmentData Enrollment data for context.
     * @param policyData Policy data for context.
     * @param context Android context for accessing the Geocoder.
     */
    public LocationCompDocument(Location location, FleetEnrollData enrollmentData, PolicyData policyData, android.content.Context context) {
        super(enrollmentData, policyData);
        this.eventAction = EVENT_ACTION;
        this.eventCategory = EVENT_CATEGORY;
        this.observerGeoLocation = new GeoLocation(location.getLatitude(), location.getLongitude());
        this.locationProvider = location.getProvider();
        this.locationAccuracy = location.getAccuracy();
        this.locationAltitude = location.getAltitude();
        this.locationSpeed = location.getSpeed();
        this.locationBearing = location.getBearing();
        this.locationTime = location.getTime();
        this.locationProvider = location.getProvider();

        // Parse the location data using GeoCoder. Location updates are coming from a background service, so it's safe to use the Geocoder here.
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !((List<?>) addresses).isEmpty()) {
                Address address = addresses.get(0);

                this.observerGeoCityName = address.getLocality();
                this.observerGeoContinentCode = address.getCountryCode();
                this.observerGeoContinentName = address.getCountryName();
                this.observerGeoCountryIsoCode = address.getCountryCode();
                this.observerGeoCountryName = address.getCountryName();
                this.observerGeoName = address.getFeatureName();
                this.observerGeoPostalCode = address.getPostalCode();
                this.observerGeoRegionIsoCode = address.getAdminArea();
                this.observerGeoRegionName = address.getAdminArea();
                this.observerGeoStreetAddress = address.getAddressLine(0);

                AppLog.d("Geocoder", "Geocoder result: " + address);

            } else {
                AppLog.w("Geocoder", "No address found for location: " + location);
            }
        } catch (IOException e) {
            AppLog.w("GeocoderError", "Geocoder failed: " + e.getMessage());
        }

    }

}
