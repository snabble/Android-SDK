package io.snabble.sdk;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

/**
 * Class representing a shop
 */
public class Shop implements Serializable, Parcelable {
    /**
     * Enum describing services provided by the shop
     */
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

    /**
     * Class describing a link
     */
    public static class Href {
        private final String href;

        public Href(String href) {
            this.href = href;
        }

        /**
         * Get the url of the link
         */
        public String getHref() {
            return href;
        }

        @NonNull
        @Override
        public String toString() {
            return "Href{" +
                    "href='" + href + '\'' +
                    '}';
        }
    }

    /**
     * Class describing when a shop opens and closes
     */
    public static class OpeningHourSpecification {
        private final String closes;
        private final String opens;
        private final String dayOfWeek;

        OpeningHourSpecification(String closes, String opens, String dayOfWeek) {
            this.closes = closes;
            this.opens = opens;
            this.dayOfWeek = dayOfWeek;
        }

        /**
         * Get the closing time as a String
         */
        public String getCloses() {
            return closes;
        }

        /**
         * Get the opening time as a String
         */
        public String getOpens() {
            return opens;
        }

        /**
         * Get the day as a String
         */
        public String getDayOfWeek() {
            return dayOfWeek;
        }

        @NonNull
        @Override
        public String toString() {
            return "OpeningHourSpecification{" +
                    "closes='" + closes + '\'' +
                    ", opens='" + opens + '\'' +
                    ", dayOfWeek='" + dayOfWeek + '\'' +
                    '}';
        }
    }

    /**
     * Class describing a potentially available wifi network in the shop
     */
    public static class CustomerNetwork {
        private final String ssid;

        CustomerNetwork(String ssid) {
            this.ssid = ssid;
        }

        /**
         * Get the SSID associated with the public wifi network
         */
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
    private String email;
    private boolean isPreLaunch;
    private Map<String, Href> links;
    @SerializedName("lat")
    private double latitude;
    @SerializedName("lon")
    private double longitude;
    private CustomerNetwork[] customerNetworks;
    private OpeningHourSpecification[] openingHoursSpecification;
    private JsonElement external;
    @SerializedName("shopServices")
    private ShopServices[] shopServices;

    Shop() {
        // for gson
    }

    /**
     * Get the unique ID associated with the shop
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the external id of the shop, used to associate this shop with shops from other backends
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Gets the list of services provided by the shop
     */
    public Service[] getServices() {
        return services;
    }

    /**
     * Gets the list of opening hours
     */
    public OpeningHourSpecification[] getOpeningHours() {
        return openingHoursSpecification;
    }

    /**
     * Gets the street of the shop
     */
    public String getStreet() {
        return street;
    }

    /**
     * Gets the zip code of the shop
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Gets the city of the shop
     */
    public String getCity() {
        return city;
    }

    /**
     * Gets the country of the shop
     */
    public String getCountry() {
        return country;
    }

    /**
     * Gets the state of the shop
     */
    public String getState() {
        return state;
    }

    /**
     * Gets the phone number of the shop
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Gets the email of the shop
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the latitude of the shop
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Gets the longitude of the shop
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Gets the latitude / longitude pair ot the shop as a Location object
     */
    public Location getLocation() {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    /**
     * Gets the name of the shop
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if the shop is only visible when setting
     * "loadActiveShops" to true in the {@link Config}
     */
    public boolean getIsPreLaunch() {
        return isPreLaunch;
    }

    /**
     * Gets the available wifi access points of the shop
     */
    public CustomerNetwork[] getCustomerNetworks() {
        return customerNetworks;
    }

    /**
     * Gets optional links
     */
    @Nullable
    public Map<String, Href> getLinks() {
        return links;
    }

    /**
     * Gets the a json document provided by external backend systems
     */
    @Nullable
    public JsonElement getExternal() {
        return external;
    }

    /**
     * Returns the shop related on-site services for the customer
     */
    @NonNull
    public ShopServices[] getShopServices() {
        return shopServices;
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
                Objects.equals(external, shop.external) &&
                Arrays.equals(shopServices, shop.shopServices);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, externalId, name, street, zipCode, city, country, state, phone, isPreLaunch, links, latitude, longitude, external);
        result = 31 * result + Arrays.hashCode(services);
        result = 31 * result + Arrays.hashCode(customerNetworks);
        result = 31 * result + Arrays.hashCode(openingHoursSpecification);
        result = 31 * result + Arrays.hashCode(shopServices);
        return result;
    }

    public String toShortString() {
        return "Shop{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @NonNull
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
                ", shopServices=" + Arrays.toString(shopServices) +
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
        InstanceCreator<Shop> creator = type -> Shop.this;

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
