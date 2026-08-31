package com.countup.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Curated, categorized icon collection for CountUp habits and milestones.
 * Icons are MIT-licensed and Apache 2.0 vector drawables, optimized as tintable 24dp vectors.
 */
const val DEFAULT_ICON: String = "person"

@Immutable
data class IconCategory(
    val id: String,
    @get:StringRes val labelRes: Int,
    val iconNames: List<String>,
)

val HEALTH_ICONS: List<String> = listOf(
    "favorite", "spa", "self_improvement", "fitness_center", "directions_walk", "water_drop", "bed", "local_hospital",
)

val HABIT_ICONS: List<String> = listOf(
    "content_cut", "smoke_free", "local_cafe", "local_bar", "restaurant", "book", "school", "pets",
)

val WORK_ICONS: List<String> = listOf(
    "terminal", "business_center", "task_alt", "timer", "lightbulb", "payments", "rocket_launch", "palette",
)

val MILESTONE_ICONS: List<String> = listOf(
    "cake", "celebration", "child_care", "home", "flight", "directions_car", "luggage", "emoji_events",
)

val SOCIAL_ICONS: List<String> = listOf(
    "person", "group", "face", "mood", "sentiment_satisfied", "sentiment_very_satisfied",
    "sentiment_neutral", "sentiment_dissatisfied", "star", "thumb_up", "share", "public",
    "recommend", "emoji_emotions", "person_add", "notification_important", "party_mode",
    "visibility", "group_add",
)

val NATURE_ICONS: List<String> = listOf(
    "park", "yard", "sunny", "bedtime", "local_florist", "sailing", "music_note", "eco",
)

/** Alias for backward compatibility with existing tests and legacy references. */
val SOCIAL_ICON_NAMES: List<String> = SOCIAL_ICONS

/** Complete list of categorized icon groups for the interactive selector. */
val ICON_CATEGORIES: List<IconCategory> = listOf(
    IconCategory("health", R.string.category_health, HEALTH_ICONS),
    IconCategory("habits", R.string.category_habits, HABIT_ICONS),
    IconCategory("work", R.string.category_work, WORK_ICONS),
    IconCategory("milestones", R.string.category_milestones, MILESTONE_ICONS),
    IconCategory("social", R.string.category_social, SOCIAL_ICONS),
    IconCategory("nature", R.string.category_nature, NATURE_ICONS),
)

/** Flattened list of all unique icon names in the collection. */
val ALL_ICON_NAMES: List<String> = ICON_CATEGORIES.flatMap { it.iconNames }.distinct()

/** Maps a stored icon name to its embedded vector drawable resource. */
@DrawableRes
fun iconRes(name: String): Int = when (name) {
    // Health & Wellness
    "spa" -> R.drawable.ic_spa
    "self_improvement" -> R.drawable.ic_self_improvement
    "fitness_center" -> R.drawable.ic_fitness_center
    "directions_walk" -> R.drawable.ic_directions_walk
    "water_drop" -> R.drawable.ic_water_drop
    "bed" -> R.drawable.ic_bed
    "local_hospital" -> R.drawable.ic_local_hospital

    // Habits & Lifestyle
    "content_cut" -> R.drawable.ic_content_cut
    "smoke_free" -> R.drawable.ic_smoke_free
    "local_cafe" -> R.drawable.ic_local_cafe
    "local_bar" -> R.drawable.ic_local_bar
    "restaurant" -> R.drawable.ic_restaurant
    "book" -> R.drawable.ic_book
    "school" -> R.drawable.ic_school
    "pets" -> R.drawable.ic_pets

    // Work & Productivity
    "terminal" -> R.drawable.ic_terminal
    "business_center" -> R.drawable.ic_business_center
    "task_alt" -> R.drawable.ic_task_alt
    "timer" -> R.drawable.ic_timer
    "lightbulb" -> R.drawable.ic_lightbulb
    "payments" -> R.drawable.ic_payments
    "rocket_launch" -> R.drawable.ic_rocket_launch
    "palette" -> R.drawable.ic_palette

    // Milestones & Events
    "cake" -> R.drawable.ic_cake
    "celebration" -> R.drawable.ic_celebration
    "child_care" -> R.drawable.ic_child_care
    "home" -> R.drawable.ic_home
    "flight" -> R.drawable.ic_flight
    "directions_car" -> R.drawable.ic_directions_car
    "luggage" -> R.drawable.ic_luggage
    "emoji_events" -> R.drawable.ic_emoji_events

    // Social & Mood
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

    // Nature & Zen
    "park" -> R.drawable.ic_park
    "yard" -> R.drawable.ic_yard
    "sunny" -> R.drawable.ic_sunny
    "bedtime" -> R.drawable.ic_bedtime
    "local_florist" -> R.drawable.ic_local_florist
    "sailing" -> R.drawable.ic_sailing
    "music_note" -> R.drawable.ic_music_note
    "eco" -> R.drawable.ic_eco

    // Default fallback
    else -> R.drawable.ic_person
}

/** Maps a stored icon name to its human-friendly TalkBack description string resource. */
@StringRes
fun iconDescriptionRes(name: String): Int = when (name) {
    // Health & Wellness
    "spa" -> R.string.cd_icon_spa
    "self_improvement" -> R.string.cd_icon_self_improvement
    "fitness_center" -> R.string.cd_icon_fitness_center
    "directions_walk" -> R.string.cd_icon_directions_walk
    "water_drop" -> R.string.cd_icon_water_drop
    "bed" -> R.string.cd_icon_bed
    "local_hospital" -> R.string.cd_icon_local_hospital

    // Habits & Lifestyle
    "content_cut" -> R.string.cd_icon_content_cut
    "smoke_free" -> R.string.cd_icon_smoke_free
    "local_cafe" -> R.string.cd_icon_local_cafe
    "local_bar" -> R.string.cd_icon_local_bar
    "restaurant" -> R.string.cd_icon_restaurant
    "book" -> R.string.cd_icon_book
    "school" -> R.string.cd_icon_school
    "pets" -> R.string.cd_icon_pets

    // Work & Productivity
    "terminal" -> R.string.cd_icon_terminal
    "business_center" -> R.string.cd_icon_business_center
    "task_alt" -> R.string.cd_icon_task_alt
    "timer" -> R.string.cd_icon_timer
    "lightbulb" -> R.string.cd_icon_lightbulb
    "payments" -> R.string.cd_icon_payments
    "rocket_launch" -> R.string.cd_icon_rocket_launch
    "palette" -> R.string.cd_icon_palette

    // Milestones & Events
    "cake" -> R.string.cd_icon_cake
    "celebration" -> R.string.cd_icon_celebration
    "child_care" -> R.string.cd_icon_child_care
    "home" -> R.string.cd_icon_home
    "flight" -> R.string.cd_icon_flight
    "directions_car" -> R.string.cd_icon_directions_car
    "luggage" -> R.string.cd_icon_luggage
    "emoji_events" -> R.string.cd_icon_emoji_events

    // Social & Mood
    "group" -> R.string.cd_icon_group
    "face" -> R.string.cd_icon_face
    "favorite" -> R.string.cd_icon_favorite
    "thumb_up" -> R.string.cd_icon_thumb_up
    "share" -> R.string.cd_icon_share
    "public" -> R.string.cd_icon_public
    "mood" -> R.string.cd_icon_mood
    "sentiment_satisfied" -> R.string.cd_icon_sentiment_satisfied
    "sentiment_very_satisfied" -> R.string.cd_icon_sentiment_very_satisfied
    "sentiment_neutral" -> R.string.cd_icon_sentiment_neutral
    "sentiment_dissatisfied" -> R.string.cd_icon_sentiment_dissatisfied
    "recommend" -> R.string.cd_icon_recommend
    "star" -> R.string.cd_icon_star
    "emoji_emotions" -> R.string.cd_icon_emoji_emotions
    "person_add" -> R.string.cd_icon_person_add
    "notification_important" -> R.string.cd_icon_notification_important
    "party_mode" -> R.string.cd_icon_party_mode
    "visibility" -> R.string.cd_icon_visibility
    "group_add" -> R.string.cd_icon_group_add

    // Nature & Zen
    "park" -> R.string.cd_icon_park
    "yard" -> R.string.cd_icon_yard
    "sunny" -> R.string.cd_icon_sunny
    "bedtime" -> R.string.cd_icon_bedtime
    "local_florist" -> R.string.cd_icon_local_florist
    "sailing" -> R.string.cd_icon_sailing
    "music_note" -> R.string.cd_icon_music_note
    "eco" -> R.string.cd_icon_eco

    // Default fallback
    else -> R.string.cd_icon_person
}
