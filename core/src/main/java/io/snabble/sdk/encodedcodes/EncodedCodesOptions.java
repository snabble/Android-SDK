package io.snabble.sdk.encodedcodes;

import android.util.SparseArray;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import io.snabble.sdk.Project;
import io.snabble.sdk.utils.JsonUtils;

public class EncodedCodesOptions {
    public static final int DEFAULT_MAX_CHARS = 2953;

    public final String prefix;
    public final SparseArray<String> prefixMap;
    public final String separator;
    public final String suffix;
    public final int maxChars;
    public final int maxCodes;
    public final String finalCode;
    public final String nextCode;
    public final String nextCodeWithCheck;
    public final boolean repeatCodes;
    public final String countSeparator;
    public final int maxSizeMm;
    public final Project project;

    private EncodedCodesOptions(String prefix, SparseArray<String> prefixMap, String separator, String suffix, int maxChars,
                                int maxCodes, String finalCode, String nextCode,
                                String nextCodeWithCheck, boolean repeatCodes, String countSeparator,
                                int maxSizeMm,
                                Project project) {
        this.prefix = prefix;
        this.prefixMap = prefixMap;
        this.separator = separator;
        this.suffix = suffix;
        this.maxChars = maxChars;
        this.maxCodes = maxCodes;
        this.finalCode = finalCode;
        this.nextCode = nextCode;
        this.nextCodeWithCheck = nextCodeWithCheck;
        this.repeatCodes = repeatCodes;
        this.countSeparator = countSeparator;
        this.maxSizeMm = maxSizeMm;
        this.project = project;
    }

    public static class Builder {
        private Project project;
        private String prefix = "";
        private SparseArray<String> prefixMap = new SparseArray<>();
        private String separator = "\n";
        private String suffix = "";
        private int maxChars = DEFAULT_MAX_CHARS;
        private String finalCode = "";
        private String nextCode = "";
        private String nextCodeWithCheck = "";
        private int maxCodes = 100;
        private boolean repeatCodes = true;
        private String countSeparator = ";";
        private int maxSizeMm;

        public Builder(Project project) {
            this.project = project;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder prefix(int index, String prefix) {
            prefixMap.put(index, prefix);
            return this;
        }

        public Builder separator(String separator) {
            this.separator = separator;
            return this;
        }

        public Builder suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        public Builder maxChars(int maxChars) {
            this.maxChars = maxChars;
            return this;
        }

        public Builder maxCodes(int maxCodes) {
            this.maxCodes = maxCodes;
            return this;
        }

        public Builder finalCode(String finalCode) {
            this.finalCode = finalCode;
            return this;
        }

        public Builder nextCode(String nextCode) {
            this.nextCode = nextCode;
            return this;
        }

        public Builder nextCodeWithCheck(String nextCodeWithCheck) {
            this.nextCodeWithCheck = nextCodeWithCheck;
            return this;
        }

        public Builder repeatCodes(boolean repeatCodes) {
            this.repeatCodes = repeatCodes;
            return this;
        }

        public Builder countSeparator(String countSeparator) {
            this.countSeparator = countSeparator;
            return this;
        }

        public Builder maxSizeMm(int maxSizeMm) {
            this.maxSizeMm = maxSizeMm;
            return this;
        }

        public EncodedCodesOptions build() {
            return new EncodedCodesOptions(prefix, prefixMap, separator, suffix, maxChars, maxCodes,
                    finalCode, nextCode, nextCodeWithCheck, repeatCodes, countSeparator, maxSizeMm, project);
        }
    }

    public static EncodedCodesOptions fromJsonObject(Project project, JsonObject object) {
        String format = JsonUtils.getStringOpt(object, "format", "simple");
        switch (format) {
            case "csv":
                return new EncodedCodesOptions.Builder(project)
                        .prefix("snabble;{qrCodeCount};{count}\n")
                        .separator("\n")
                        .suffix("")
                        .repeatCodes(false)
                        .countSeparator(";")
                        .maxCodes(100)
                        .build();
            case "csv_globus":
                return new EncodedCodesOptions.Builder(project)
                        .prefix("snabble;\n")
                        .separator("\n")
                        .suffix("")
                        .repeatCodes(false)
                        .countSeparator(";")
                        .maxCodes(100)
                        .build();
            case "ikea":
                EncodedCodesOptions options = project.getEncodedCodesOptions();
                int maxCodes = 45;
                int maxChars = EncodedCodesOptions.DEFAULT_MAX_CHARS;
                if (options != null) {
                    maxCodes = options.maxCodes;
                    maxChars = options.maxChars;
                }

                String prefix = "9100003\u001d100{qrCodeCount}\u001d240";

                String prefixWithCustomerCard = "9100003\u001d100{qrCodeCount}";
                if (project.getCustomerCardId() != null) {
                    prefixWithCustomerCard += "\u001d92" + project.getCustomerCardId();
                }
                prefixWithCustomerCard += "\u001d240";

                return new EncodedCodesOptions.Builder(project)
                                .prefix(prefix)
                                .prefix(0, prefixWithCustomerCard)
                                .separator("\u001d240")
                                .suffix("")
                                .maxCodes(maxCodes)
                                .maxChars(maxChars)
                                .build();
            case "simple":
            default:
                return new EncodedCodesOptions.Builder(project)
                        .prefix(JsonUtils.getStringOpt(object, "prefix", ""))
                        .separator(JsonUtils.getStringOpt(object, "separator", "\n"))
                        .suffix(JsonUtils.getStringOpt(object, "suffix", ""))
                        .maxCodes(JsonUtils.getIntOpt(object, "maxCodes", 100))
                        .finalCode(JsonUtils.getStringOpt(object, "finalCode", ""))
                        .nextCode(JsonUtils.getStringOpt(object, "nextCode", ""))
                        .nextCodeWithCheck(JsonUtils.getStringOpt(object, "nextCodeWithCheck", ""))
                        .maxSizeMm(JsonUtils.getIntOpt(object, "maxSizeMM", -1))
                        .maxChars(JsonUtils.getIntOpt(object, "maxChars", EncodedCodesOptions.DEFAULT_MAX_CHARS))
                        .build();
        }
    }
}
