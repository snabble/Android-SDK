package io.snabble.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

public class Shop implements Serializable, Parcelable {
    public enum Service {
        @SerializedName("euro")
        EURO_PAYMENT,
        @SerializedName("bakery")
        BAKERY,
        @SerializedName("butcher")
        BUTCHER,
        @SerializedName("creditcard")
        CREDIT_CARD,
        @SerializedName("fishmonger")
        FISH,
        @SerializedName("nonfood")
        NON_FOOD,
        @SerializedName("parking")
        PARKING
    }

    public class Href {
        public String href;

        @Override
        public String toString() {
            return "Href{" +
                    "href='" + href + '\'' +
                    '}';
        }
    }

    public class OpeningHourSpecification {
        public String closes;
        public String opens;
        public String dayOfWeek;

        public String getCloses() {
            return closes;
        }

        public String getOpens() {
            return opens;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        @Override
        public String toString() {
            return "OpeningHourSpecification{" +
                    "closes='" + closes + '\'' +
                    ", opens='" + opens + '\'' +
                    ", dayOfWeek='" + dayOfWeek + '\'' +
                    '}';
        }
    }

    private String id;
    private String externalId;
    private String name;
    private Service[] services;
    private String street;
    @SerializedName("zip")
    private String zipCode;
    private String city;
    private String country;
    private String state;
    private String phone;
    private Map<String, Href> links;
    @SerializedName("lat")
    private double latitude;
    @SerializedName("lon")
    private double longitude;
    private OpeningHourSpecification[] openingHoursSpecification;
    private JsonElement external;

    public Shop() {

    }

    public String getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public Service[] getServices() {
        return services;
    }

    public OpeningHourSpecification[] getOpeningHours() {
        return openingHoursSpecification;
    }

    public String getStreet() {
        return street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getPhone() {
        return phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    public Map<String, Href> getLinks() {
        return links;
    }

    public JsonElement getExternal() {
        return external;
    }

    static Shop[] fromJson(JsonElement json) {
        try {
            return GsonHolder.get().fromJson(json, Shop[].class);
        } catch (Exception e) {
            Logger.e("Could not read shops json");
            return null;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Shop shop = (Shop) o;

        return id.equals(shop.id);
    }

    public String toShortString() {
        return "Shop{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public String toString() {
        return "Shop{" +
                "id='" + id + '\'' +
                ", externalId='" + externalId + '\'' +
                ", name='" + name + '\'' +
                ", services=" + Arrays.toString(services) +
                ", street='" + street + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", state='" + state + '\'' +
                ", phone='" + phone + '\'' +
                ", links=" + links +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", openingHoursSpecification=" + Arrays.toString(openingHoursSpecification) +
                ", external=" + external +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String str = GsonHolder.get().toJson(this);
        dest.writeString(str);
    }

    protected Shop(Parcel in) {
        InstanceCreator<Shop> creator = new InstanceCreator<Shop>() {
            public Shop createInstance(Type type) { return Shop.this; }
        };

        Gson gson = new GsonBuilder().registerTypeAdapter(Shop.class, creator).create();
        gson.fromJson(in.readString(), Shop.class);
    }

    public static final Parcelable.Creator<Shop> CREATOR = new Parcelable.Creator<Shop>() {
        @Override
        public Shop createFromParcel(Parcel source) {
            return new Shop(source);
        }

        @Override
        public Shop[] newArray(int size) {
            return new Shop[size];
        }
    };
}
