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
import java.util.Objects;

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

    public static class Href {
        private final String href;

        public Href(String href) {
            this.href = href;
        }

        public String getHref() {
            return href;
        }

        @Override
        public String toString() {
            return "Href{" +
                    "href='" + href + '\'' +
                    '}';
        }
    }

    public static class OpeningHourSpecification {
        private final String closes;
        private final String opens;
        private final String dayOfWeek;

        public OpeningHourSpecification(String closes, String opens, String dayOfWeek) {
            this.closes = closes;
            this.opens = opens;
            this.dayOfWeek = dayOfWeek;
        }

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

    public static class CustomerNetwork {
        private final String ssid;

        public CustomerNetwork(String ssid) {
            this.ssid = ssid;
        }

        public String getSsid() {
            return ssid;
        }

        @Override
        public String toString() {
            return "CustomerNetwork{" +
                    "ssid='" + ssid + '\'' +
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
    private boolean isPreLaunch;
    private Map<String, Href> links;
    @SerializedName("lat")
    private double latitude;
    @SerializedName("lon")
    private double longitude;
    private CustomerNetwork[] customerNetworks;
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

    public boolean getIsPreLaunch() {
        return isPreLaunch;
    }

    public CustomerNetwork[] getCustomerNetworks() {
        return customerNetworks;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shop shop = (Shop) o;
        return isPreLaunch == shop.isPreLaunch &&
                Double.compare(shop.latitude, latitude) == 0 &&
                Double.compare(shop.longitude, longitude) == 0 &&
                Objects.equals(id, shop.id) &&
                Objects.equals(externalId, shop.externalId) &&
                Objects.equals(name, shop.name) &&
                Arrays.equals(services, shop.services) &&
                Objects.equals(street, shop.street) &&
                Objects.equals(zipCode, shop.zipCode) &&
                Objects.equals(city, shop.city) &&
                Objects.equals(country, shop.country) &&
                Objects.equals(state, shop.state) &&
                Objects.equals(phone, shop.phone) &&
                Objects.equals(links, shop.links) &&
                Arrays.equals(customerNetworks, shop.customerNetworks) &&
                Arrays.equals(openingHoursSpecification, shop.openingHoursSpecification) &&
                Objects.equals(external, shop.external);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, externalId, name, street, zipCode, city, country, state, phone, isPreLaunch, links, latitude, longitude, external);
        result = 31 * result + Arrays.hashCode(services);
        result = 31 * result + Arrays.hashCode(customerNetworks);
        result = 31 * result + Arrays.hashCode(openingHoursSpecification);
        return result;
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
                ", isPreLaunch=" + isPreLaunch +
                ", links=" + links +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", customerNetworks=" + Arrays.toString(customerNetworks) +
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
