package io.snabble.sdk.codes.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.codes.templates.groups.*;

public class CodeTemplate {
    private List<Group> groups;

    public CodeTemplate(String pattern) {
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
                int length = -1;
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

                Group group;

                switch (type) {
                    case "code":
                        if (length == -1) {
                            switch (subType) {
                                case "ean8": group = new EAN8Group(this); break;
                                case "ean13": group = new EAN13Group(this); break;
                                case "ean14": group = new EAN14Group(this);break;
                                default: throw new IllegalArgumentException("Unknown code type: " + subType);
                            }
                        } else {
                            group = new CodeGroup(this, length);
                        }
                        break;
                    case "weight":
                        group = new WeightGroup(this, length);
                        break;
                    case "price":
                        group = new PriceGroup(this, length);
                        break;
                    case "i":
                        group = new InternalChecksumGroup(this);
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

        // check if required groups are present
        for (Group group : groups) {
            if (!group.checkDependencies()) {
                throw new IllegalArgumentException(group.getClass().getSimpleName() + " is missing dependent groups");
            }
        }
    }

    public <T extends Group> T getComponent(Class<? extends Group> clazz) {
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

        return new ScannableCode(match);
    }

    public static CodeTemplate parse(String pattern) {
        try {
            return new CodeTemplate(pattern);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


