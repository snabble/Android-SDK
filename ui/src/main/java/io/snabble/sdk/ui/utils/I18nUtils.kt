package io.snabble.sdk.ui.utils

import android.content.res.Resources
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.Project
import java.lang.RuntimeException

object I18nUtils {
    @JvmStatic
    fun getIdentifierForProject(res: Resources, project: Project?, id: Int): Int {
        try {
            if (project != null) {
                var entryName = res.getResourceEntryName(id)
                val typeName = res.getResourceTypeName(id)
                val packageName = res.getResourcePackageName(id)
                entryName = project.id.replace("-", ".") + "." + entryName
                val newId = res.getIdentifier(entryName, typeName, packageName)
                if (newId != 0) {
                    return newId
                }
            }
        } catch (e: RuntimeException) {
            return id
        }
        return id
    }

    @JvmStatic
    fun getIdentifier(res: Resources, id: Int) =
        getIdentifierForProject(res, instance.checkedInProject.getValue(), id)

    fun getStringForProject(res: Resources, project: Project?, id: String): String? {
        try {
            if (project != null) {
                val typeName = "string"
                val packageName = instance.application.packageName
                val entryName = project.id.replace("-", ".") + "." + id
                val newId = res.getIdentifier(entryName, typeName, packageName)
                if (newId != 0) {
                    return res.getString(newId)
                }
            }
        } catch (e: RuntimeException) {
            return null
        }
        return null
    }

    fun getString(res: Resources, id: String): String? =
        getStringForProject(res, instance.checkedInProject.getValue(), id)

    fun Resources.getStringForProject(project: Project?, id: Int, vararg args: Any?) =
        getString(getIdentifierForProject(this, project, id), *args)

    fun Resources.getQuantityStringForProject(project: Project?, id: Int, quantity: Int, vararg args: Any?) =
        getQuantityString(getIdentifierForProject(this, project, id), quantity, *args)
}