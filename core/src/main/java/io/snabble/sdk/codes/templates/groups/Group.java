package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public abstract class Group {
    private CodeTemplate template;
    private int length;
    private String data;

    public Group(CodeTemplate template, int length) {
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
            return true;
        }

        return false;
    }

    public String data() {
        return data;
    }

    public boolean checkDependencies() {
        return true;
    }

    public boolean validate() {
        return true;
    }
}
