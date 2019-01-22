package io.snabble.sdk.codes.templates;

import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.EAN14;
import io.snabble.sdk.codes.ScannableCode;

public class CodeTemplate {
    public abstract class Component {
        private CodeTemplate template;
        private int length;
        private String data;

        public Component(CodeTemplate template, int length) {
            this.template = template;
            this.length = length;
        }

        public CodeTemplate getTemplate() {
            return template;
        }

        public int length() {
            return length;
        }

        public void reset() {
            data = null;
        }

        public boolean apply(String input) {
            if (input != null && input.length() == length) {
                data = input;

                if (validateInput()) {
                    return true;
                } else {
                    reset();
                    return false;
                }
            }

            return false;
        }

        public String data() {
            return data;
        }

        public Class<? extends Component> getUniqueComponent() {
            return null;
        }

        public Class<? extends Component> requiresComponent() {
            return null;
        }

        public boolean validateComponents() {
            return true;
        }

        public boolean validateInput() {
            return true;
        }
    }

    public class PlainTextComponent extends Component {
        private String match;

        public PlainTextComponent(CodeTemplate template, String match) {
            super(template, match.length());
            this.match = match;
        }

        @Override
        public boolean validateInput() {
            return data().equals(match);
        }
    }

    public class IgnoreComponent extends Component {
        public IgnoreComponent(CodeTemplate template, int length) {
            super(template, length);
        }
    }

    public class NumberComponent extends Component {
        public NumberComponent(CodeTemplate template, int length) {
            super(template, length);
        }

        public int number(String input) {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        @Override
        public boolean validateInput() {
            try {
                Integer.parseInt(data());
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
    }

    public class PriceComponent extends NumberComponent {
        public PriceComponent(CodeTemplate template, int length) {
            super(template, length);
        }

        @Override
        public Class<? extends Component> getUniqueComponent() {
            return PriceComponent.class;
        }
    }

    public class WeightComponent extends NumberComponent {
        public WeightComponent(CodeTemplate template, int length) {
            super(template, length);
        }

        @Override
        public Class<? extends Component> getUniqueComponent() {
            return WeightComponent.class;
        }
    }

    public class CodeComponent extends Component {
        public CodeComponent(CodeTemplate template, int length) {
            super(template, length);
        }

        @Override
        public Class<? extends Component> getUniqueComponent() {
            return CodeComponent.class;
        }
    }

    public class EAN8Component extends CodeComponent {
        public EAN8Component(CodeTemplate template) {
            super(template, 8);
        }
    }

    public class EAN13Component extends CodeComponent {
        public EAN13Component(CodeTemplate template) {
            super(template, 13);
        }

        @Override
        public boolean validateInput() {
            return EAN13.isEan13(data());
        }
    }

    public class EAN14Component extends CodeComponent {
        public EAN14Component(CodeTemplate template) {
            super(template, 14);
        }

        @Override
        public boolean validateInput() {
            return EAN14.isEan14(data());
        }
    }

    public class InternalChecksumComponent extends NumberComponent {
        public InternalChecksumComponent(CodeTemplate template) {
            super(template, 1);
        }

        @Override
        public Class<? extends Component> getUniqueComponent() {
            return InternalChecksumComponent.class;
        }

        @Override
        public Class<? extends Component> requiresComponent() {
            return WeightComponent.class;
        }

        @Override
        public boolean validateComponents() {
            WeightComponent weightComponent = getTemplate().getComponent(WeightComponent.class);
            if (weightComponent == null || weightComponent.length() != 5) {
                return false;
            }

            return true;
        }

        // TODO onvalidateafter/before?
        @Override
        public boolean validateInput() {
            WeightComponent weightComponent = getTemplate().getComponent(WeightComponent.class);
            if (weightComponent != null) {
                String weight = weightComponent.data();
                return EAN13.internalChecksum(weight, 0) == number(data());
            }

            return false;
        }
    }

    private List<Component> components;

    public CodeTemplate(String pattern) {
        components = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        boolean isInGroup = false;

        for (int i=0; i<pattern.length(); i++) {
            char c = pattern.charAt(i);

            if (c == '{') {
                isInGroup = true;
                if (sb.length() > 0) {
                    components.add(new PlainTextComponent(this, sb.toString()));
                    sb.setLength(0);
                }
            } else if (c == '}') {
                if (!isInGroup) {
                    throw new IllegalArgumentException("Missing '{' before closing group");
                }
                isInGroup = false;
                String group = sb.toString();
                sb.setLength(0);

                String type = "_";
                int length = -1;
                String subType = "";
                String[] split = group.split(":");
                if (split.length > 0) {
                    type = split[0];
                }

                if (split.length == 2) {
                    try {
                        subType = split[1];
                        length = Integer.parseInt(subType);
                    } catch (NumberFormatException ignored) { }
                }

                Component component;

                switch (type) {
                    case "code":
                        if (length == -1) {
                            switch (subType) {
                                case "ean8": component = new EAN8Component(this); break;
                                case "ean13": component = new EAN13Component(this); break;
                                case "ean14": component = new EAN14Component(this); break;
                                default: throw new IllegalArgumentException("Unknown code type: " + subType);
                            }
                        } else {
                            component = new CodeComponent(this, length);
                        }
                        break;
                    case "weight":
                        component = new WeightComponent(this, length);
                        break;
                    case "price":
                        component = new PriceComponent(this, length);
                        break;
                    case "i":
                        component = new InternalChecksumComponent(this);
                        break;
                    default:
                        if (type.startsWith("_")) {
                            component = new IgnoreComponent(this, type.length());
                        } else {
                            throw new IllegalArgumentException("Invalid component group: " + group);
                        }
                        break;
                }

                // check for uniqueness
                if (checkUnique(component)) {
                    throw new IllegalArgumentException(component.getClass().getSimpleName() + " is a unique component");
                }

                components.add(component);
            } else {
                sb.append(c);
            }
        }

        if (isInGroup) {
            throw new IllegalArgumentException("Unclosed group");
        }

        // check if required components are present
        for (Component component : components) {
            if (!component.validateComponents()) {
                throw new IllegalArgumentException(component.getClass().getSimpleName() + " is requiring " + component.requiresComponent().getSimpleName());
            }
        }
    }

    private boolean checkUnique(Component obj) {
        if (obj.getUniqueComponent() != null) {
            for (Component component : components) {
                if (component.getUniqueComponent() == obj.getUniqueComponent()) {
                    return true;
                }
            }
        }

        return false;
    }

    private Component findComponentByClass(Class<? extends Component> clazz) {
        for (Component component : components) {
            if (component.getClass() == clazz) {
                return component;
            }
        }

        return null;
    }

    public <T extends Component> T getComponent(Class<? extends Component> clazz) {
        return (T)findComponentByClass(clazz);
    }

    public ScannableCode match(String match) {
        int start = 0;
        for (Component component : components) {
            int end = start + component.length();
            if (match.length() < end) {
                return null;
            }

            String input = match.substring(start, end);
            if (!component.apply(input)) {
                return null;
            }
            start += component.length();
        }

        for (Component component : components) {
            if (component instanceof CodeComponent) {
                return new ScannableCode(component.data());
            }
        }

        return null;
    }

    public static CodeTemplate parse(String pattern) {
        try {
            return new CodeTemplate(pattern);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


