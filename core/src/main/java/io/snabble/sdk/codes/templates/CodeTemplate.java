package io.snabble.sdk.codes.templates;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.codes.templates.groups.CodeGroup;
import io.snabble.sdk.codes.templates.groups.ConstantCodeGroup;
import io.snabble.sdk.codes.templates.groups.EAN13Group;
import io.snabble.sdk.codes.templates.groups.EAN13InternalChecksumGroup;
import io.snabble.sdk.codes.templates.groups.EAN14Group;
import io.snabble.sdk.codes.templates.groups.EAN8Group;
import io.snabble.sdk.codes.templates.groups.EANChecksumGroup;
import io.snabble.sdk.codes.templates.groups.EmbedDecimalGroup;
import io.snabble.sdk.codes.templates.groups.EmbedGroup;
import io.snabble.sdk.codes.templates.groups.Group;
import io.snabble.sdk.codes.templates.groups.IgnoreGroup;
import io.snabble.sdk.codes.templates.groups.PlainTextGroup;
import io.snabble.sdk.codes.templates.groups.PriceGroup;
import io.snabble.sdk.codes.templates.groups.WildcardGroup;

/**
 * Class for parsing of code templates.
 * <p>
 * Example: "2{code:5}{i}{code:5}{ec}", resembling a standard EAN13 containing embedded weight or prices
 * <p>
 * Valid groups
 * <p>
 * code         | either a known format (ean8, ean13 or ean14), or a length
 * <p>
 * embed        | embedded data in the code. This may be a weight, amount or price,
 * depending on the encodingUnit of the scanned code
 * <p>
 * Supports decimal lengths such as "4.3",
 * which tells the parser that there are 4 decimal and 3 fractional digits encoded.
 * <p>
 * embed100     | embedded data in the code that must be multiplied by 100 (e.g. for an embedded price in Euros)
 * <p>
 * price        | price of one referenceUnit of the product
 * <p>
 * _            | ignore length character(s)
 * <p>
 * i            | length is always 1, other values will be ignored.
 * Represents the internal 5-digit checksum of an EAN-13.
 * Presence of this property requires that the embed property is also present and has a length of 5.
 * <p>
 * ec           | length is always 1, other values will be ignored, and must be the last component of a template.
 * Represents the check digit of an EAN-8, EAN-13 or EAN-14 code.
 */
public class CodeTemplate {
    private String pattern;
    private List<Group> groups;
    private String name;
    private String matchedCode;

    public CodeTemplate(String name, String pattern) {
        this.name = name;
        this.pattern = pattern;
        groups = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        Set<String> groupNames = new HashSet<>();
        boolean isInGroup = false;

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);

            if (c == '{') {
                isInGroup = true;
                if (sb.length() > 0) {
                    groups.add(new PlainTextGroup(this, sb.toString()));
                    sb.setLength(0);
                }
            } else if (c == '}') {
                if (!isInGroup) {
                    throw new IllegalArgumentException("Missing '{' before closing group");
                }
                isInGroup = false;
                String templateGroup = sb.toString();
                sb.setLength(0);

                String type = "_";
                int length = 0;
                String subType = null;
                String[] parts = templateGroup.split(":");
                boolean isConstantCode = false;
                if (parts.length > 0) {
                    type = parts[0];

                    if (parts.length == 1) {
                        parts = templateGroup.split("=");

                        if (parts.length == 2) {
                            type = parts[0];
                            isConstantCode = true;
                            subType = parts[1];
                        }
                    }
                }

                if (!isConstantCode) {
                    if (parts.length == 2) {
                        try {
                            length = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            subType = parts[1];
                        }

                        if (subType == null && length < 1) {
                            throw new IllegalArgumentException("Invalid group length: " + length);
                        }
                    }

                    if (templateGroup.endsWith(":")) {
                        throw new IllegalArgumentException("Missing group length in " + templateGroup);
                    }
                }

                Group group;

                switch (type) {
                    case "code":
                        if (subType != null && isConstantCode) {
                            group = new ConstantCodeGroup(this, subType);
                            group.apply(subType);
                        } else {
                            if (subType != null) {
                                switch (subType) {
                                    case "ean8":
                                        group = new EAN8Group(this);
                                        break;
                                    case "ean13":
                                        group = new EAN13Group(this);
                                        break;
                                    case "ean14":
                                        group = new EAN14Group(this);
                                        break;
                                    case "*":
                                        group = new WildcardGroup(this, 0);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown code type: " + subType);
                                }
                            } else {
                                group = new CodeGroup(this, length);
                            }
                        }
                        break;
                    case "embed":
                        if (subType != null) {
                            String[] split = subType.split("\\.");
                            if (split.length == 2) {
                                try {
                                    int integerParts = Integer.parseInt(split[0]);
                                    int fractionalParts = Integer.parseInt(split[1]);

                                    group = new EmbedDecimalGroup(this, integerParts, fractionalParts);
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Illegal embed format: " + e.getMessage());
                                }
                            } else {
                                throw new IllegalArgumentException("Illegal embed format: only one fractional part is allowed");
                            }
                        } else {
                            group = new EmbedGroup(this, length, 1);
                        }
                        break;
                    case "embed100":
                        group = new EmbedGroup(this, length, 100);
                        break;
                    case "price":
                        group = new PriceGroup(this, length);
                        break;
                    case "i":
                        group = new EAN13InternalChecksumGroup(this);
                        break;
                    case "ec":
                        if (length() != 7 && length() != 12) {
                            throw new IllegalArgumentException("{ec} only supports ean 8 and ean 13 types");
                        }

                        if (i < pattern.length() - 1) {
                            throw new IllegalArgumentException("{ec} always must be the last entry");
                        }

                        group = new EANChecksumGroup(this);
                        break;
                    case "*":
                        group = new WildcardGroup(this, 0);
                        break;
                    default:
                        if (type.startsWith("_")) {
                            if (length == 0) {
                                length = type.length();
                            }
                            group = new IgnoreGroup(this, length);
                        } else {
                            throw new IllegalArgumentException("Invalid group: " + templateGroup);
                        }
                        break;
                }

                // check for uniqueness
                if (!type.startsWith("_")) {
                    if (groupNames.contains(type)) {
                        throw new IllegalArgumentException("Duplicate group: " + type);
                    }
                }

                groups.add(group);
                groupNames.add(type);
            } else {
                sb.append(c);
            }
        }

        if (isInGroup) {
            throw new IllegalArgumentException("Unclosed group");
        }

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Empty template");
        }

        // check if required groups are present
        for (Group group : groups) {
            if (!group.checkDependencies()) {
                throw new IllegalArgumentException(group.getClass().getSimpleName() + " is missing dependent groups");
            }
        }
    }

    public String getName() {
        return name;
    }

    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public <T extends Group> T getGroup(Class<? extends Group> clazz) {
        for (Group group : groups) {
            try {
                group.getClass().asSubclass(clazz);
                return (T) group;
            } catch (ClassCastException ignored) {
            }
        }

        return null;
    }

    public void reset() {
        for (Group group : groups) {
            group.reset();
        }
    }

    /**
     * Matches a code to the template, extracting information that resides in the code.
     */
    public CodeTemplate match(String match) {
        matchedCode = match;
        return this;
    }

    /**
     * Sets the code of the code group
     */
    public CodeTemplate code(String code) {
        CodeGroup codeGroup = getGroup(CodeGroup.class);

        if (codeGroup != null) {
            codeGroup.apply(code);
        } else {
            WildcardGroup wildcardGroup = getGroup(WildcardGroup.class);
            if (wildcardGroup != null) {
                wildcardGroup.apply(code);
            }
        }

        return this;
    }

    /**
     * The embedded data that will be embedded in the code, if the code has a embed group
     */
    public CodeTemplate embed(int embeddedData) {
        EmbedGroup embedGroup = getGroup(EmbedGroup.class);

        if (embedGroup != null) {
            embedGroup.applyInt(embeddedData);
        }

        return this;
    }

    /**
     * Generates a parsed {@link ScannedCode} containing code and embedded data.
     */
    public ScannedCode buildCode() {
        ScannedCode.Builder builder = new ScannedCode.Builder(name);

        if (matchedCode != null) {
            reset();

            int start = 0;
            for (int i = 0; i < groups.size(); i++) {
                Group group = groups.get(i);
                if (i == groups.size() - 1 && group instanceof IgnoreGroup) {
                    break;
                }

                if (group instanceof WildcardGroup) {
                    ((WildcardGroup) group).setLength(matchedCode.length() - start);
                }

                int end = start + group.length();
                if (matchedCode.length() < end) {
                    matchedCode = null;
                    reset();
                    return null;
                }

                String input = matchedCode.substring(start, end);
                if (!group.apply(input)) {
                    matchedCode = null;
                    reset();
                    return null;
                }

                start += group.length();
            }

            builder.setScannedCode(matchedCode);
            matchedCode = null;
        } else {
            EAN13InternalChecksumGroup ean13InternalChecksumGroup = getGroup(EAN13InternalChecksumGroup.class);
            EANChecksumGroup eanChecksumGroup = getGroup(EANChecksumGroup.class);
            if (ean13InternalChecksumGroup != null) {
                ean13InternalChecksumGroup.recalculate();
            }

            if (eanChecksumGroup != null) {
                eanChecksumGroup.recalculate();
            }

            IgnoreGroup ignoreGroup = getGroup(IgnoreGroup.class);
            if (ignoreGroup != null) {
                ignoreGroup.apply(StringUtils.repeat('0', ignoreGroup.length()));
            }

            builder.setScannedCode(string());
        }

        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            if (i == groups.size() - 1 && group instanceof IgnoreGroup) {
                break;
            }

            if (!group.validate()) {
                reset();
                return null;
            }
        }

        for (Group group : groups) {
            if (group instanceof EmbedGroup) {
                builder.setEmbeddedData(((EmbedGroup) group).number());
            }

            if (group instanceof EmbedDecimalGroup) {
                builder.setEmbeddedDecimalData(((EmbedDecimalGroup) group).decimal());
            }

            if (group instanceof PriceGroup) {
                builder.setPrice(((PriceGroup) group).number());
            }

            if (group instanceof CodeGroup) {
                builder.setLookupCode(group.string());
            }
        }

        reset();
        return builder.create();
    }

    public String string() {
        StringBuilder sb = new StringBuilder(length());

        for (Group group : groups) {
            sb.append(group.string());
        }

        return sb.toString();
    }

    public int length() {
        int len = 0;

        for (Group group : groups) {
            len += group.length();
        }

        return len;
    }

    public String getPattern() {
        return pattern;
    }
}


