package io.snabble.sdk;

import java.util.List;
import java.util.Locale;

public class TermsOfService {
    public String updatedAt;
    public String version;
    public List<Variant> variants;

    public String getHtmlLinkForSystemLanguage() {
        Locale defaultLocale = Locale.getDefault();
        for (Variant variant : variants) {
            if (new Locale(variant.language).getLanguage().equals(defaultLocale.getLanguage())) {
                return Snabble.getInstance().absoluteUrl(variant.getUrl());
            }
        }

        for (Variant variant : variants) {
            if (variant.isDefault) {
                return Snabble.getInstance().absoluteUrl(variant.getUrl());
            }
        }

        if (variants.size() > 0) {
            return Snabble.getInstance().absoluteUrl(variants.get(0).getUrl());
        }

        return null;
    }

    public static class Variant {
        public boolean isDefault;
        public String language;
        public Links links;

        public String getUrl() {
            if (links != null && links.content != null) {
                return links.content.href;
            }

            return  null;
        }
    }

    public static class Links {
        private Content content;
    }

    public static class Content {
        private String href;
    }
}


