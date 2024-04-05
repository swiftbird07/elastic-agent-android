package de.swiftbird.elasticandroid;

import android.location.Location;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity
public class LocationCompDocument extends ElasticDocument {

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

    @SerializedName("observer.geo.location.long")
    @ColumnInfo(name = "observerGeoLocationLong")
    public double observerGeoLocationLong;

    @SerializedName("observer.geo.location.lat")
    @ColumnInfo(name = "observerGeoLocationLat")
    public double observerGeoLocationLat;

    // Some other fields that are not ECS but useful for our use case

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

    public LocationCompDocument(Location location, FleetEnrollData enrollmentData, PolicyData policyData) {
        super(enrollmentData, policyData);
        this.eventAction = "location-update";
        this.eventCategory = "location";
        this.observerGeoLocationLat = location.getLatitude();
        this.observerGeoLocationLong = location.getLongitude();
        this.locationProvider = location.getProvider();
        this.locationAccuracy = location.getAccuracy();
        this.locationAltitude = location.getAltitude();
        this.locationSpeed = location.getSpeed();
        this.locationBearing = location.getBearing();
        this.locationTime = location.getTime();
        this.locationProvider = location.getProvider();
    }
}
