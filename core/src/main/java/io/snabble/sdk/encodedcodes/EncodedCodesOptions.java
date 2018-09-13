package io.snabble.sdk.encodedcodes;

public class EncodedCodesOptions {
    public final String prefix;
    public final String separator;
    public final String suffix;
    public final int maxChars;
    public final int maxCodes;
    public final String finalCode;
    public final String nextCode;
    public final String nextCodeWithCheck;

    private EncodedCodesOptions(String prefix, String separator, String suffix, int maxChars,
                               int maxCodes, String finalCode, String nextCode, String nextCodeWithCheck) {
        this.prefix = prefix;
        this.separator = separator;
        this.suffix = suffix;
        this.maxChars = maxChars;
        this.maxCodes = maxCodes;
        this.finalCode = finalCode;
        this.nextCode = nextCode;
        this.nextCodeWithCheck = nextCodeWithCheck;
    }

    public static class Builder {
        private String prefix = "";
        private String separator = "\n";
        private String suffix = "";
        private int maxChars = 2953;
        private String finalCode = "";
        private String nextCode = "";
        private String nextCodeWithCheck = "";
        private int maxCodes = 100;

        public Builder prefix(String prefix) {
            this.prefix = prefix;
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

        public EncodedCodesOptions build() {
            return new EncodedCodesOptions(prefix, separator, suffix, maxChars, maxCodes,
                                           finalCode, nextCode, nextCodeWithCheck);
        }
    }
}
