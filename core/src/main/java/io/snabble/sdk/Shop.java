package io.snabble.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

import io.snabble.sdk.utils.Logger;

public class Shop implements Serializable {
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
    }

    private String id;
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

    public String getId() {
        return id;
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
            Gson gson = new GsonBuilder()
                    .create();

            return gson.fromJson(json, Shop[].class);
        } catch (Exception e) {
            Logger.e("Could not read shops json");
            return null;
        }
    }

    public static class Builder {
        private String id;
        private String name;
        private Shop.Service[] services;
        private String street;
        private String zipCode;
        private String city;
        private String country;
        private String state;
        private String phone;
        private double latitude;
        private double longitude;
        private OpeningHourSpecification[] openingHoursSpecification;
        private JsonElement external;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder services(Shop.Service[] services) {
            this.services = services;
            return this;
        }

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder latLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder openingHours(OpeningHourSpecification[] openingHours) {
            this.openingHoursSpecification = openingHours;
            return this;
        }

        public Builder external(JsonElement external) {
            this.external = external;
            return this;
        }

        public Shop create() {
            Shop shop = new Shop();

            shop.id = id;
            shop.name = name;
            shop.services = services;
            shop.street = street;
            shop.zipCode = zipCode;
            shop.city = city;
            shop.country = country;
            shop.state = state;
            shop.phone = phone;
            shop.latitude = latitude;
            shop.longitude = longitude;
            shop.openingHoursSpecification = openingHoursSpecification;
            shop.external = external;

            return shop;
        }
    }
}
