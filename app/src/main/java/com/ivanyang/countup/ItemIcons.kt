package com.ivanyang.countup

import androidx.annotation.DrawableRes

/**
 * Curated set of Material "Social" category icons, embedded as vector drawables.
 * Source: fonts.google.com/icons (Material Icons) — Apache License 2.0,
 * free for commercial use. Each icon is a distinct, tintable 24dp vector.
 */
const val DEFAULT_ICON: String = "person"

val SOCIAL_ICON_NAMES: List<String> = listOf(
    "person", "group", "face", "favorite", "thumb_up", "share", "public", "mood",
    "sentiment_satisfied", "sentiment_very_satisfied", "sentiment_neutral",
    "sentiment_dissatisfied", "recommend", "star", "emoji_emotions", "person_add",
    "notification_important", "party_mode", "visibility", "group_add",
)

/** Maps a stored icon name to its embedded drawable resource. */
@DrawableRes
fun iconRes(name: String): Int = when (name) {
    "group" -> R.drawable.ic_group
    "face" -> R.drawable.ic_face
    "favorite" -> R.drawable.ic_favorite
    "thumb_up" -> R.drawable.ic_thumb_up
    "share" -> R.drawable.ic_share
    "public" -> R.drawable.ic_public
    "mood" -> R.drawable.ic_mood
    "sentiment_satisfied" -> R.drawable.ic_sentiment_satisfied
    "sentiment_very_satisfied" -> R.drawable.ic_sentiment_very_satisfied
    "sentiment_neutral" -> R.drawable.ic_sentiment_neutral
    "sentiment_dissatisfied" -> R.drawable.ic_sentiment_dissatisfied
    "recommend" -> R.drawable.ic_recommend
    "star" -> R.drawable.ic_star
    "emoji_emotions" -> R.drawable.ic_emoji_emotions
    "person_add" -> R.drawable.ic_person_add
    "notification_important" -> R.drawable.ic_notification_important
    "party_mode" -> R.drawable.ic_party_mode
    "visibility" -> R.drawable.ic_visibility
    "group_add" -> R.drawable.ic_group_add
    // "person" and any unknown/missing name fall back to the person icon.
    else -> R.drawable.ic_person
}
