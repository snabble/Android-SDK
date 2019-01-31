package io.snabble.sdk.codes.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.codes.templates.groups.*;

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
                if (parts.length > 0) {
                    type = parts[0];
                }

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

                Group group;

                switch (type) {
                    case "code":
                        if (subType != null) {
                            switch (subType) {
                                case "ean8": group = new EAN8Group(this); break;
                                case "ean13": group = new EAN13Group(this); break;
                                case "ean14": group = new EAN14Group(this); break;
                                case "*": group = new WildcardGroup(this, 0); break;
                                default: throw new IllegalArgumentException("Unknown code type: " + subType);
                            }
                        } else {
                            group = new CodeGroup(this, length);
                        }
                        break;
                    case "embed":
                        group = new EmbedGroup(this, length, 1);
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
            if (group.getClass() == clazz) {
                return (T) group;
            }
        }

        return null;
    }

    // TODO remove mutable state
    public void reset() {
        for (Group group : groups) {
            group.reset();
        }
    }

    public CodeTemplate match(String match) {
        matchedCode = match;
        return this;
    }

    public CodeTemplate code(String code) {
        CodeGroup codeGroup = getGroup(CodeGroup.class);

        if (codeGroup != null) {
            codeGroup.apply(code);
        }

        return this;
    }

    public CodeTemplate embed(int embeddedData) {
        PlainTextGroup plainTextGroup = getGroup(PlainTextGroup.class);
        EmbedGroup embedGroup = getGroup(EmbedGroup.class);
        EAN13InternalChecksumGroup ean13InternalChecksumGroup = getGroup(EAN13InternalChecksumGroup.class);
        EANChecksumGroup eanChecksumGroup = getGroup(EANChecksumGroup.class);

        if (plainTextGroup != null) {
            plainTextGroup.apply(plainTextGroup.plainText());
        }

        if (embedGroup != null) {
            embedGroup.applyInt(embeddedData);
        }

        if (ean13InternalChecksumGroup != null) {
            ean13InternalChecksumGroup.recalculate();
        }

        if (eanChecksumGroup != null) {
            eanChecksumGroup.recalculate();
        }

        return this;
    }

    public ScannableCode buildCode() {
        ScannableCode.Builder builder = new ScannableCode.Builder(name);

        if (matchedCode != null) {
            reset();

            int start = 0;
            for (Group group : groups) {
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
            PlainTextGroup plainTextGroup = getGroup(PlainTextGroup.class);
            if (plainTextGroup != null) {
                plainTextGroup.apply(plainTextGroup.plainText());
            }

            builder.setScannedCode(string());
        }

        for (Group group : groups) {
            if (!group.validate()) {
                reset();
                return null;
            }
        }

        for (Group group : groups) {
            if (group instanceof EmbedGroup) {
                builder.setEmbeddedData(((EmbedGroup) group).number());
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


