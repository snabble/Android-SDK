package io.snabble.sdk.ui.utils;

import android.content.res.Resources;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;

public class I18nUtils {
    public static int getIdentifierForProject(Resources res, Project project, int id) {
        try {
            if (project != null) {
                String entryName = res.getResourceEntryName(id);
                String typeName = res.getResourceTypeName(id);
                String packageName = res.getResourcePackageName(id);

                entryName = project.getId().replace("-", ".") + "." + entryName;
                int newId = res.getIdentifier(entryName, typeName, packageName);
                if (newId != 0) {
                    return newId;
                }
            }
        } catch (RuntimeException e) {
            return id;
        }

        return id;
    }

    public static int getIdentifier(Resources res, int id) {
        return getIdentifierForProject(res, Snabble.getInstance().getCheckedInProject().getValue(), id);
    }

    public static String getStringForProject(Resources res, Project project, String id) {
        try {
            if (project != null) {
                String typeName = "string";
                String packageName = Snabble.getInstance().getApplication().getPackageName();

                String entryName = project.getId().replace("-", ".") + "." + id;
                int newId = res.getIdentifier(entryName, typeName, packageName);
                if (newId != 0) {
                    return res.getString(newId);
                }
            }
        } catch (RuntimeException e) {
            return null;
        }

        return null;
    }

    public static String getString(Resources res, String id) {
        return getStringForProject(res, Snabble.getInstance().getCheckedInProject().getValue(), id);
    }
}
