package io.snabble.sdk.ui.utils

import android.content.res.Resources
import androidx.annotation.RestrictTo
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import java.lang.RuntimeException

/**
 * Localization utils and extension functions to get project specific localized strings and quantity strings.
 */
object I18nUtils {
    /**
     * Resolve a project specific resource id with a project reference.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    @JvmStatic
    fun getIdentifierForProject(res: Resources, project: Project?, id: Int): Int =
        getIdentifierForProject(res, project?.id, id)

    /**
     * Resolve a project specific resource id with its project id.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    @JvmStatic
    fun getIdentifierForProject(res: Resources, projectId: String?, id: Int): Int {
        try {
            if (projectId != null) {
                var entryName = res.getResourceEntryName(id)
                val typeName = res.getResourceTypeName(id)
                val packageName = res.getResourcePackageName(id)
                entryName = projectId.replace("-", ".") + "." + entryName
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

    /**
     * Resolve a project specific resource id for the current checked in project.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    @JvmStatic
    fun getIdentifier(res: Resources, id: Int) =
        getIdentifierForProject(res, Snabble.checkedInProject.value, id)

    private fun getStringForProject(res: Resources, project: Project?, id: String): String? {
        try {
            if (project != null) {
                val typeName = "string"
                val packageName = Snabble.application.packageName
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

    // Internal special case where there is never a project independent localization
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getString(res: Resources, id: String): String? =
        getStringForProject(res, instance.checkedInProject.getValue(), id)

    /**
     * Extension function to get a project specific string with a project reference.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    fun Resources.getStringForProject(project: Project?, id: Int, vararg args: Any?) =
        getString(getIdentifierForProject(this, project?.id, id), *args)

    /**
     * Extension function to get a project specific quantity string with a project reference.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    fun Resources.getQuantityStringForProject(project: Project?, id: Int, quantity: Int, vararg args: Any?) =
        getQuantityString(getIdentifierForProject(this, project?.id, id), quantity, *args)

    /**
     * Extension function to get a project specific string with its id.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    fun Resources.getStringForProject(projectId: String?, id: Int, vararg args: Any?) =
        getString(getIdentifierForProject(this, projectId, id), *args)

    /**
     * Extension function to get a project specific quantity string with its id.
     *
     * Returns project specific localization or the input id when the project is not set or the project has no special
     * localization.
     */
    fun Resources.getQuantityStringForProject(projectId: String?, id: Int, quantity: Int, vararg args: Any?) =
        getQuantityString(getIdentifierForProject(this, projectId, id), quantity, *args)
}