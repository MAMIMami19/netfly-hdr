package com.nuvio.tv.ui.netflyhdr.profiles

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.nuvio.tv.R
import com.nuvio.tv.data.remote.supabase.AvatarCatalogItem

/**
 * Netfly HDR's built-in avatar set (design §11).
 *
 * Netfly's own 114 avatars are copyrighted characters (Simpsons, Star Wars,
 * Marvel, WWE) and are NEVER shipped here. These 24 avatars are original
 * abstract/geometric designs generated for Netfly HDR (256x256 PNGs in
 * `res/drawable/netfly_avatar_XX.png`): no characters, no logos, no text.
 *
 * They are exposed as [AvatarCatalogItem]s so the existing avatar flows
 * (profile create/edit picker, profile row, sidebar) pick them up through
 * [com.nuvio.tv.data.remote.supabase.AvatarRepository.getAvatarCatalog]
 * exactly like the remote catalog. Images resolve through Coil's built-in
 * `android.resource://` support, so no network is needed and they work
 * offline; the URI authority is the runtime package name, which also covers
 * the `.debug` applicationId.
 *
 * Pure data: no Compose dependency, so the data layer may reference it.
 */
object NetflyLocalAvatars {

    /** Category shown as its own tab in the base avatar picker grid. */
    const val CATEGORY = "netfly"

    /** Id prefix for every local avatar (`netfly-local-01` ... `netfly-local-24`). */
    const val ID_PREFIX = "netfly-local-"

    val drawables: List<Int> = listOf(
        R.drawable.netfly_avatar_01,
        R.drawable.netfly_avatar_02,
        R.drawable.netfly_avatar_03,
        R.drawable.netfly_avatar_04,
        R.drawable.netfly_avatar_05,
        R.drawable.netfly_avatar_06,
        R.drawable.netfly_avatar_07,
        R.drawable.netfly_avatar_08,
        R.drawable.netfly_avatar_09,
        R.drawable.netfly_avatar_10,
        R.drawable.netfly_avatar_11,
        R.drawable.netfly_avatar_12,
        R.drawable.netfly_avatar_13,
        R.drawable.netfly_avatar_14,
        R.drawable.netfly_avatar_15,
        R.drawable.netfly_avatar_16,
        R.drawable.netfly_avatar_17,
        R.drawable.netfly_avatar_18,
        R.drawable.netfly_avatar_19,
        R.drawable.netfly_avatar_20,
        R.drawable.netfly_avatar_21,
        R.drawable.netfly_avatar_22,
        R.drawable.netfly_avatar_23,
        R.drawable.netfly_avatar_24
    )

    fun avatarId(index: Int): String = "$ID_PREFIX%02d".format(index + 1)

    fun isLocalAvatarId(id: String?): Boolean = id?.startsWith(ID_PREFIX) == true

    /** Drawable resource for a local avatar id, or null for remote/unknown ids. */
    fun drawableResForId(id: String?): Int? {
        if (!isLocalAvatarId(id)) return null
        val number = id!!.removePrefix(ID_PREFIX).toIntOrNull() ?: return null
        if (number < 1 || number > drawables.size) return null
        return drawables[number - 1]
    }

    /** Coil-loadable `android.resource://` URL for a drawable resource. */
    fun imageUrl(context: Context, drawableRes: Int): String {
        val resources = context.resources
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(resources.getResourceTypeName(drawableRes))
            .appendPath(resources.getResourceEntryName(drawableRes))
            .build()
            .toString()
    }

    /** The local avatars as catalog items, in gallery order. */
    fun catalog(context: Context): List<AvatarCatalogItem> =
        drawables.mapIndexed { index, drawableRes ->
            AvatarCatalogItem(
                id = avatarId(index),
                displayName = context.getString(R.string.netfly_avatar_name, index + 1),
                imageUrl = imageUrl(context, drawableRes),
                category = CATEGORY,
                sortOrder = index
            )
        }
}
