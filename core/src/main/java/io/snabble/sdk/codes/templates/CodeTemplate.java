package io.snabble.sdk.codes.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.snabble.sdk.Project;
import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.codes.templates.groups.*;

public class CodeTemplate {
    private String pattern;
    private List<Group> groups;
    private String name;

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
                Integer length = null;
                String subType = "";
                String[] split = templateGroup.split(":");
                if (split.length > 0) {
                    type = split[0];
                }

                if (split.length == 2) {
                    try {
                        subType = split[1];
                        length = Integer.parseInt(subType);
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (length != null && length < 1) {
                    throw new IllegalArgumentException("Invalid group length: " + length);
                }

                Group group;

                switch (type) {
                    case "code":
                        if (length == null) {
                            switch (subType) {
                                case "ean8": group = new EAN8Group(this); break;
                                case "ean13": group = new EAN13Group(this); break;
                                case "ean14": group = new EAN14Group(this); break;
                                default: throw new IllegalArgumentException("Unknown code type: " + subType);
                            }
                        } else {
                            group = new CodeGroup(this, length);
                        }
                        break;
                    case "embed":
                        group = new EmbedGroup(this, length);
                        break;
                    case "price":
                        group = new PriceGroup(this, length);
                        break;
                    case "i":
                        group = new InternalChecksumGroup(this);
                        break;
                    case "*":
                        group = new WildcardGroup(this, 0);
                        break;
                    default:
                        if (type.startsWith("_")) {
                            group = new IgnoreGroup(this, type.length());
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

    public <T extends Group> T getGroup(Class<? extends Group> clazz) {
        for (Group group : groups) {
            if (group.getClass() == clazz) {
                return (T) group;
            }
        }

        return null;
    }

    public ScannableCode match(String match) {
        int start = 0;
        for (Group group : groups) {
            if (group instanceof WildcardGroup) {
                ((WildcardGroup) group).setLength(match.length());
            }

            int end = start + group.length();
            if (match.length() < end) {
                return null;
            }

            String input = match.substring(start, end);
            group.reset();
            if (!group.apply(input)) {
                return null;
            }

            start += group.length();
        }

        for (Group group : groups) {
            if (!group.validate()) {
                return null;
            }
        }

        ScannableCode.Builder builder = new ScannableCode.Builder(this);
        builder.setScannedCode(match);

        for (Group group : groups) {
            if (group instanceof EmbedGroup) {
                builder.setEmbeddedData(((EmbedGroup) group).number());
            }

            if (group instanceof CodeGroup) {
                builder.setLookupCode(group.data());
            }
        }

        return builder.create();
    }

    private int length() {
        int len = 0;
        for (Group group : groups) {
            len += group.length();
        }
        return len;
    }

    public String getPattern() {
        return pattern;
    }

    public static CodeTemplate parse(String name, String pattern) {
        try {
            return new CodeTemplate(name, pattern);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


