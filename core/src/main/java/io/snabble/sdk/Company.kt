package io.snabble.sdk;

public class Company {
    public final String city;
    public final String country;
    public final String name;
    public final String street;
    public final String zip;

    public Company(String city, String country, String name, String street, String zip) {
        this.city = city;
        this.country = country;
        this.name = name;
        this.street = street;
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public String getStreet() {
        return street;
    }

    public String getZip() {
        return zip;
    }

    @Override
    public String toString() {
        return "Company{" +
                "city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", name='" + name + '\'' +
                ", street='" + street + '\'' +
                ", zip='" + zip + '\'' +
                '}';
    }
}

