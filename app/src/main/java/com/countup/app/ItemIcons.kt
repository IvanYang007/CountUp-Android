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
data class IconEntry(
    val name: String,
    @get:DrawableRes val drawableRes: Int,
    @get:StringRes val descriptionRes: Int,
)

@Immutable
data class IconCategory(
    val id: String,
    @get:StringRes val labelRes: Int,
    val iconNames: List<String>,
)

private val HEALTH_ICON_ENTRIES: List<IconEntry> = listOf(
    IconEntry("favorite", R.drawable.ic_favorite, R.string.cd_icon_favorite),
    IconEntry("spa", R.drawable.ic_spa, R.string.cd_icon_spa),
    IconEntry("self_improvement", R.drawable.ic_self_improvement, R.string.cd_icon_self_improvement),
    IconEntry("fitness_center", R.drawable.ic_fitness_center, R.string.cd_icon_fitness_center),
    IconEntry("directions_walk", R.drawable.ic_directions_walk, R.string.cd_icon_directions_walk),
    IconEntry("water_drop", R.drawable.ic_water_drop, R.string.cd_icon_water_drop),
    IconEntry("bed", R.drawable.ic_bed, R.string.cd_icon_bed),
    IconEntry("local_hospital", R.drawable.ic_local_hospital, R.string.cd_icon_local_hospital),
    IconEntry("tooth", R.drawable.ic_tooth, R.string.cd_icon_tooth),
    IconEntry("pill", R.drawable.ic_pill, R.string.cd_icon_pill),
    IconEntry("sneaker", R.drawable.ic_sneaker, R.string.cd_icon_sneaker),
    IconEntry("shower", R.drawable.ic_shower, R.string.cd_icon_shower),
    IconEntry("scales", R.drawable.ic_scales, R.string.cd_icon_scales),
    IconEntry("heartbeat", R.drawable.ic_heartbeat, R.string.cd_icon_heartbeat),
    IconEntry("fasting_plate", R.drawable.ic_fasting_plate, R.string.cd_icon_fasting_plate),
    IconEntry("hourglass_spark", R.drawable.ic_hourglass_spark, R.string.cd_icon_hourglass_spark),
    IconEntry("period_moon", R.drawable.ic_period_moon, R.string.cd_icon_period_moon),
    IconEntry("period_lotus", R.drawable.ic_period_lotus, R.string.cd_icon_period_lotus),
    IconEntry("cold_tub", R.drawable.ic_cold_tub, R.string.cd_icon_cold_tub),
    IconEntry("frost_spark", R.drawable.ic_frost_spark, R.string.cd_icon_frost_spark),
    IconEntry("skincare_dropper", R.drawable.ic_skincare_dropper, R.string.cd_icon_skincare_dropper),
    IconEntry("sun_shield", R.drawable.ic_sun_shield, R.string.cd_icon_sun_shield),
    IconEntry("rx_bottle", R.drawable.ic_rx_bottle, R.string.cd_icon_rx_bottle),
    IconEntry("vaccine_syringe", R.drawable.ic_vaccine_syringe, R.string.cd_icon_vaccine_syringe),
    IconEntry("glasses_round", R.drawable.ic_glasses_round, R.string.cd_icon_glasses_round),
    IconEntry("eye_focus", R.drawable.ic_eye_focus, R.string.cd_icon_eye_focus),
    IconEntry("dental_floss", R.drawable.ic_dental_floss, R.string.cd_icon_dental_floss),
    IconEntry("tooth_sparkle", R.drawable.ic_tooth_sparkle, R.string.cd_icon_tooth_sparkle),
    IconEntry("yoga_mat", R.drawable.ic_yoga_mat, R.string.cd_icon_yoga_mat),
    IconEntry("flex_wave", R.drawable.ic_flex_wave, R.string.cd_icon_flex_wave),
    IconEntry("hair_tint_brush", R.drawable.ic_hair_tint_brush, R.string.cd_icon_hair_tint_brush),
    IconEntry("hair_swirl", R.drawable.ic_hair_swirl, R.string.cd_icon_hair_swirl),
    IconEntry("mind_heart", R.drawable.ic_mind_heart, R.string.cd_icon_mind_heart),
    IconEntry("kintsugi_bowl", R.drawable.ic_kintsugi_bowl, R.string.cd_icon_kintsugi_bowl),
)

private val HABIT_ICON_ENTRIES: List<IconEntry> = listOf(
    IconEntry("content_cut", R.drawable.ic_content_cut, R.string.cd_icon_content_cut),
    IconEntry("smoke_free", R.drawable.ic_smoke_free, R.string.cd_icon_smoke_free),
    IconEntry("local_cafe", R.drawable.ic_local_cafe, R.string.cd_icon_local_cafe),
    IconEntry("local_bar", R.drawable.ic_local_bar, R.string.cd_icon_local_bar),
    IconEntry("restaurant", R.drawable.ic_restaurant, R.string.cd_icon_restaurant),
    IconEntry("book", R.drawable.ic_book, R.string.cd_icon_book),
    IconEntry("school", R.drawable.ic_school, R.string.cd_icon_school),
    IconEntry("pets", R.drawable.ic_pets, R.string.cd_icon_pets),
    IconEntry("guitar", R.drawable.ic_guitar, R.string.cd_icon_guitar),
    IconEntry("game_controller", R.drawable.ic_game_controller, R.string.cd_icon_game_controller),
    IconEntry("bicycle", R.drawable.ic_bicycle, R.string.cd_icon_bicycle),
    IconEntry("cooking_pot", R.drawable.ic_cooking_pot, R.string.cd_icon_cooking_pot),
    IconEntry("knife", R.drawable.ic_knife, R.string.cd_icon_knife),
    IconEntry("washing_machine", R.drawable.ic_washing_machine, R.string.cd_icon_washing_machine),
    IconEntry("broom", R.drawable.ic_broom, R.string.cd_icon_broom),
    IconEntry("yarn", R.drawable.ic_yarn, R.string.cd_icon_yarn),
    IconEntry("phone_slash", R.drawable.ic_phone_slash, R.string.cd_icon_phone_slash),
    IconEntry("sprout_device", R.drawable.ic_sprout_device, R.string.cd_icon_sprout_device),
    IconEntry("sugar_slash", R.drawable.ic_sugar_slash, R.string.cd_icon_sugar_slash),
    IconEntry("clean_apple", R.drawable.ic_clean_apple, R.string.cd_icon_clean_apple),
    IconEntry("sober_glass_inverted", R.drawable.ic_sober_glass_inverted, R.string.cd_icon_sober_glass_inverted),
    IconEntry("sober_dawn", R.drawable.ic_sober_dawn, R.string.cd_icon_sober_dawn),
    IconEntry("journal_ribbon", R.drawable.ic_journal_ribbon, R.string.cd_icon_journal_ribbon),
    IconEntry("fountain_pen", R.drawable.ic_fountain_pen, R.string.cd_icon_fountain_pen),
    IconEntry("wallet_padlock", R.drawable.ic_wallet_padlock, R.string.cd_icon_wallet_padlock),
    IconEntry("coin_sprout", R.drawable.ic_coin_sprout, R.string.cd_icon_coin_sprout),
    IconEntry("alarm_five_am", R.drawable.ic_alarm_five_am, R.string.cd_icon_alarm_five_am),
    IconEntry("dawn_ridge", R.drawable.ic_dawn_ridge, R.string.cd_icon_dawn_ridge),
    IconEntry("translate_bubbles", R.drawable.ic_translate_bubbles, R.string.cd_icon_translate_bubbles),
    IconEntry("language_bridge", R.drawable.ic_language_bridge, R.string.cd_icon_language_bridge),
    IconEntry("watering_can", R.drawable.ic_watering_can, R.string.cd_icon_watering_can),
    IconEntry("monstera_pot", R.drawable.ic_monstera_pot, R.string.cd_icon_monstera_pot),
    IconEntry("pomodoro_timer", R.drawable.ic_pomodoro_timer, R.string.cd_icon_pomodoro_timer),
    IconEntry("zen_bell", R.drawable.ic_zen_bell, R.string.cd_icon_zen_bell),
    IconEntry("book_stack", R.drawable.ic_book_stack, R.string.cd_icon_book_stack),
    IconEntry("reading_beacon", R.drawable.ic_reading_beacon, R.string.cd_icon_reading_beacon),
)

private val WORK_ICON_ENTRIES: List<IconEntry> = listOf(
    IconEntry("terminal", R.drawable.ic_terminal, R.string.cd_icon_terminal),
    IconEntry("business_center", R.drawable.ic_business_center, R.string.cd_icon_business_center),
    IconEntry("task_alt", R.drawable.ic_task_alt, R.string.cd_icon_task_alt),
    IconEntry("timer", R.drawable.ic_timer, R.string.cd_icon_timer),
    IconEntry("lightbulb", R.drawable.ic_lightbulb, R.string.cd_icon_lightbulb),
    IconEntry("payments", R.drawable.ic_payments, R.string.cd_icon_payments),
    IconEntry("rocket_launch", R.drawable.ic_rocket_launch, R.string.cd_icon_rocket_launch),
    IconEntry("palette", R.drawable.ic_palette, R.string.cd_icon_palette),
    IconEntry("wrench", R.drawable.ic_wrench, R.string.cd_icon_wrench),
    IconEntry("identification_card", R.drawable.ic_identification_card, R.string.cd_icon_identification_card),
    IconEntry("piggy_bank", R.drawable.ic_piggy_bank, R.string.cd_icon_piggy_bank),
    IconEntry("newspaper", R.drawable.ic_newspaper, R.string.cd_icon_newspaper),
    IconEntry("target", R.drawable.ic_target, R.string.cd_icon_target),
    IconEntry("tax_form", R.drawable.ic_tax_form, R.string.cd_icon_tax_form),
    IconEntry("tax_balance", R.drawable.ic_tax_balance, R.string.cd_icon_tax_balance),
    IconEntry("bank_check", R.drawable.ic_bank_check, R.string.cd_icon_bank_check),
    IconEntry("coin_stack", R.drawable.ic_coin_stack, R.string.cd_icon_coin_stack),
    IconEntry("briefcase_star", R.drawable.ic_briefcase_star, R.string.cd_icon_briefcase_star),
    IconEntry("career_ladder", R.drawable.ic_career_ladder, R.string.cd_icon_career_ladder),
    IconEntry("receipt_cut", R.drawable.ic_receipt_cut, R.string.cd_icon_receipt_cut),
    IconEntry("chain_broken", R.drawable.ic_chain_broken, R.string.cd_icon_chain_broken),
    IconEntry("card_cut", R.drawable.ic_card_cut, R.string.cd_icon_card_cut),
    IconEntry("padlock_open", R.drawable.ic_padlock_open, R.string.cd_icon_padlock_open),
    IconEntry("shipping_crate", R.drawable.ic_shipping_crate, R.string.cd_icon_shipping_crate),
    IconEntry("beacon_light", R.drawable.ic_beacon_light, R.string.cd_icon_beacon_light),
    IconEntry("clipboard_check", R.drawable.ic_clipboard_check, R.string.cd_icon_clipboard_check),
    IconEntry("gauge_high", R.drawable.ic_gauge_high, R.string.cd_icon_gauge_high),
)

private val MILESTONE_ICON_ENTRIES: List<IconEntry> = listOf(
    IconEntry("cake", R.drawable.ic_cake, R.string.cd_icon_cake),
    IconEntry("celebration", R.drawable.ic_celebration, R.string.cd_icon_celebration),
    IconEntry("child_care", R.drawable.ic_child_care, R.string.cd_icon_child_care),
    IconEntry("home", R.drawable.ic_home, R.string.cd_icon_home),
    IconEntry("flight", R.drawable.ic_flight, R.string.cd_icon_flight),
    IconEntry("directions_car", R.drawable.ic_directions_car, R.string.cd_icon_directions_car),
    IconEntry("luggage", R.drawable.ic_luggage, R.string.cd_icon_luggage),
    IconEntry("emoji_events", R.drawable.ic_emoji_events, R.string.cd_icon_emoji_events),
    IconEntry("key", R.drawable.ic_key, R.string.cd_icon_key),
    IconEntry("ticket", R.drawable.ic_ticket, R.string.cd_icon_ticket),
    IconEntry("baby_carriage", R.drawable.ic_baby_carriage, R.string.cd_icon_baby_carriage),
    IconEntry("medal", R.drawable.ic_medal, R.string.cd_icon_medal),
    IconEntry("crown", R.drawable.ic_crown, R.string.cd_icon_crown),
    IconEntry("wedding_ring", R.drawable.ic_wedding_ring, R.string.cd_icon_wedding_ring),
    IconEntry("champagne_flutes", R.drawable.ic_champagne_flutes, R.string.cd_icon_champagne_flutes),
    IconEntry("house_keyhole", R.drawable.ic_house_keyhole, R.string.cd_icon_house_keyhole),
    IconEntry("moving_box", R.drawable.ic_moving_box, R.string.cd_icon_moving_box),
    IconEntry("mended_heart", R.drawable.ic_mended_heart, R.string.cd_icon_mended_heart),
    IconEntry("bird_flight", R.drawable.ic_bird_flight, R.string.cd_icon_bird_flight),
    IconEntry("mortarboard_cap", R.drawable.ic_mortarboard_cap, R.string.cd_icon_mortarboard_cap),
    IconEntry("diploma_scroll", R.drawable.ic_diploma_scroll, R.string.cd_icon_diploma_scroll),
    IconEntry("paw_heart", R.drawable.ic_paw_heart, R.string.cd_icon_paw_heart),
    IconEntry("pet_collar", R.drawable.ic_pet_collar, R.string.cd_icon_pet_collar),
    IconEntry("passport_book", R.drawable.ic_passport_book, R.string.cd_icon_passport_book),
    IconEntry("compass_rose", R.drawable.ic_compass_rose, R.string.cd_icon_compass_rose),
    IconEntry("steering_wheel", R.drawable.ic_steering_wheel, R.string.cd_icon_steering_wheel),
    IconEntry("highway_horizon", R.drawable.ic_highway_horizon, R.string.cd_icon_highway_horizon),
    IconEntry("tattoo_machine", R.drawable.ic_tattoo_machine, R.string.cd_icon_tattoo_machine),
    IconEntry("ink_constellation", R.drawable.ic_ink_constellation, R.string.cd_icon_ink_constellation),
    IconEntry("beach_chair", R.drawable.ic_beach_chair, R.string.cd_icon_beach_chair),
    IconEntry("soaring_kite", R.drawable.ic_soaring_kite, R.string.cd_icon_soaring_kite),
)

private val SOCIAL_ICON_ENTRIES: List<IconEntry> = listOf(
    IconEntry("person", R.drawable.ic_person, R.string.cd_icon_person),
    IconEntry("group", R.drawable.ic_group, R.string.cd_icon_group),
    IconEntry("face", R.drawable.ic_face, R.string.cd_icon_face),
    IconEntry("mood", R.drawable.ic_mood, R.string.cd_icon_mood),
    IconEntry("sentiment_satisfied", R.drawable.ic_sentiment_satisfied, R.string.cd_icon_sentiment_satisfied),
    IconEntry("sentiment_very_satisfied", R.drawable.ic_sentiment_very_satisfied, R.string.cd_icon_sentiment_very_satisfied),
    IconEntry("sentiment_neutral", R.drawable.ic_sentiment_neutral, R.string.cd_icon_sentiment_neutral),
    IconEntry("sentiment_dissatisfied", R.drawable.ic_sentiment_dissatisfied, R.string.cd_icon_sentiment_dissatisfied),
    IconEntry("star", R.drawable.ic_star, R.string.cd_icon_star),
    IconEntry("thumb_up", R.drawable.ic_thumb_up, R.string.cd_icon_thumb_up),
    IconEntry("share", R.drawable.ic_share, R.string.cd_icon_share),
    IconEntry("public", R.drawable.ic_public, R.string.cd_icon_public),
    IconEntry("recommend", R.drawable.ic_recommend, R.string.cd_icon_recommend),
    IconEntry("emoji_emotions", R.drawable.ic_emoji_emotions, R.string.cd_icon_emoji_emotions),
    IconEntry("person_add", R.drawable.ic_person_add, R.string.cd_icon_person_add),
    IconEntry("notification_important", R.drawable.ic_notification_important, R.string.cd_icon_notification_important),
    IconEntry("party_mode", R.drawable.ic_party_mode, R.string.cd_icon_party_mode),
    IconEntry("visibility", R.drawable.ic_visibility, R.string.cd_icon_visibility),
    IconEntry("group_add", R.drawable.ic_group_add, R.string.cd_icon_group_add),
    IconEntry("handshake", R.drawable.ic_handshake, R.string.cd_icon_handshake),
    IconEntry("gift", R.drawable.ic_gift, R.string.cd_icon_gift),
    IconEntry("envelope", R.drawable.ic_envelope, R.string.cd_icon_envelope),
    IconEntry("twin_candles", R.drawable.ic_twin_candles, R.string.cd_icon_twin_candles),
    IconEntry("shared_umbrella", R.drawable.ic_shared_umbrella, R.string.cd_icon_shared_umbrella),
    IconEntry("stage_mic", R.drawable.ic_stage_mic, R.string.cd_icon_stage_mic),
    IconEntry("stadium_beam", R.drawable.ic_stadium_beam, R.string.cd_icon_stadium_beam),
    IconEntry("clapperboard", R.drawable.ic_clapperboard, R.string.cd_icon_clapperboard),
    IconEntry("film_reel", R.drawable.ic_film_reel, R.string.cd_icon_film_reel),
)

private val NATURE_ICON_ENTRIES: List<IconEntry> = listOf(
    IconEntry("park", R.drawable.ic_park, R.string.cd_icon_park),
    IconEntry("yard", R.drawable.ic_yard, R.string.cd_icon_yard),
    IconEntry("sunny", R.drawable.ic_sunny, R.string.cd_icon_sunny),
    IconEntry("bedtime", R.drawable.ic_bedtime, R.string.cd_icon_bedtime),
    IconEntry("local_florist", R.drawable.ic_local_florist, R.string.cd_icon_local_florist),
    IconEntry("sailing", R.drawable.ic_sailing, R.string.cd_icon_sailing),
    IconEntry("music_note", R.drawable.ic_music_note, R.string.cd_icon_music_note),
    IconEntry("eco", R.drawable.ic_eco, R.string.cd_icon_eco),
    IconEntry("mountains", R.drawable.ic_mountains, R.string.cd_icon_mountains),
    IconEntry("campfire", R.drawable.ic_campfire, R.string.cd_icon_campfire),
    IconEntry("tent", R.drawable.ic_tent, R.string.cd_icon_tent),
    IconEntry("compass", R.drawable.ic_compass, R.string.cd_icon_compass),
    IconEntry("camera", R.drawable.ic_camera, R.string.cd_icon_camera),
    IconEntry("camper_van", R.drawable.ic_camper_van, R.string.cd_icon_camper_van),
    IconEntry("mountain_pass", R.drawable.ic_mountain_pass, R.string.cd_icon_mountain_pass),
)

private val HEALTH_ICONS: List<String> = HEALTH_ICON_ENTRIES.map { it.name }
private val HABIT_ICONS: List<String> = HABIT_ICON_ENTRIES.map { it.name }
private val WORK_ICONS: List<String> = WORK_ICON_ENTRIES.map { it.name }
private val MILESTONE_ICONS: List<String> = MILESTONE_ICON_ENTRIES.map { it.name }
val SOCIAL_ICONS: List<String> = SOCIAL_ICON_ENTRIES.map { it.name }
private val NATURE_ICONS: List<String> = NATURE_ICON_ENTRIES.map { it.name }

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

private val ALL_ICON_ENTRIES: List<IconEntry> =
    HEALTH_ICON_ENTRIES + HABIT_ICON_ENTRIES + WORK_ICON_ENTRIES + MILESTONE_ICON_ENTRIES + SOCIAL_ICON_ENTRIES + NATURE_ICON_ENTRIES

private val ICON_MAP: Map<String, IconEntry> = ALL_ICON_ENTRIES.associateBy { it.name }

/** Flattened list of all unique icon names in the collection. */
val ALL_ICON_NAMES: List<String> = ALL_ICON_ENTRIES.map { it.name }.distinct()

/**
 * Fast keyword mapping pair coupling a contextual icon with a harmonious card color preset.
 */
@Immutable
data class KeywordStyleMatch(val icon: String, val cardColor: String)

/**
 * Curated deterministic keyword rules for zero-thinking auto-styling.
 */
private val KEYWORD_STYLE_RULES: List<Pair<List<String>, KeywordStyleMatch>> = listOf(
    // Pillar A: Health, Wellness & Body
    listOf("period", "menstrual", "menstruation", "cycle", "ovulation", "经期", "月经", "大姨妈", "生理期", "排卵") to KeywordStyleMatch("period_moon", "ink_crimson"),
    listOf("cold plunge", "ice bath", "cryo", "cryotherapy", "冷水澡", "冰浴", "冷疗") to KeywordStyleMatch("cold_tub", "paper_indigo"),
    listOf("skincare", "serum", "spf", "sunscreen", "moisturizer", "护肤", "精华", "防晒", "面霜", "水乳") to KeywordStyleMatch("skincare_dropper", "paper_sage"),
    listOf("medication", "prescription", "rx", "vaccine", "allergy shot", "处方", "吃药", "疫苗", "打针", "过敏针") to KeywordStyleMatch("rx_bottle", "paper_terracotta"),
    listOf("glasses", "optometry", "eye exam", "contact lens", "contacts", "配镜", "验光", "隐形眼镜", "近视", "查视力") to KeywordStyleMatch("glasses_round", "paper_sage"),
    listOf("floss", "whitening", "teeth whitening", "dental checkup", "牙线", "美白牙齿", "牙齿美白") to KeywordStyleMatch("dental_floss", "paper_sage"),
    listOf("stretch", "stretching", "mobility", "yoga", "pilates", "拉伸", "瑜伽", "普拉提", "柔韧") to KeywordStyleMatch("yoga_mat", "sage_forest"),
    listOf("hair tint", "hair dye", "scalp", "hair color", "染发", "烫发", "头皮护理", "做头发") to KeywordStyleMatch("hair_tint_brush", "sage_ochre"),
    listOf("therapy", "therapist", "mental health", "counseling", "psychology", "心理咨询", "心理医生", "疗愈", "心语") to KeywordStyleMatch("mind_heart", "sage_forest"),

    // Pillar B: Habits & Discipline
    listOf("sugar free", "no sugar", "zero sugar", "quit sugar", "戒糖", "断糖", "控糖", "无糖") to KeywordStyleMatch("sugar_slash", "paper_sage"),
    listOf("sober", "sobriety", "alcohol free", "quit drinking", "戒酒", "清醒", "滴酒不沾") to KeywordStyleMatch("sober_glass_inverted", "paper_indigo"),
    listOf("journal", "diary", "journaling", "reflection", "日记", "手帐", "写日记", "复盘") to KeywordStyleMatch("journal_ribbon", "paper_terracotta"),
    listOf("no spend", "saving challenge", "money saving", "budget challenge", "不消费", "零支出", "省钱挑战", "无消费日") to KeywordStyleMatch("wallet_padlock", "ink_gold"),
    listOf("5 am", "early bird", "wake early", "early rise", "5am", "早起打卡", "晨起", "五点起") to KeywordStyleMatch("alarm_five_am", "ink_gold"),
    listOf("duolingo", "language", "vocab", "vocabulary", "spanish", "japanese", "french", "german", "背单词", "学英语", "外语", "多邻国", "学日语") to KeywordStyleMatch("translate_bubbles", "paper_indigo"),
    listOf("water plant", "houseplant", "watering", "monstera", "succulent", "浇花", "绿植", "植物浇水", "多肉", "龟背竹") to KeywordStyleMatch("watering_can", "sage_forest"),
    listOf("pomodoro", "deep work", "focus sprint", "timer sprint", "番茄钟", "深度工作", "专注时间") to KeywordStyleMatch("pomodoro_timer", "ink_crimson"),
    listOf("reading streak", "read book", "books finished", "book club", "读书打卡", "读完一本书", "阅读打卡") to KeywordStyleMatch("book_stack", "paper_sage"),

    // Pillar C: Milestones & Relationships
    listOf("moving", "relocation", "new house", "new apartment", "new home", "搬家", "乔迁", "新居", "交房") to KeywordStyleMatch("house_keyhole", "ink_gold"),
    listOf("breakup", "healing", "closure", "moving on", "分手", "疗伤", "走出阴霾", "释怀") to KeywordStyleMatch("mended_heart", "sage_forest"),
    listOf("grad", "graduation", "thesis", "defense", "degree", "diploma", "毕业", "答辩", "学位", "毕业典礼", "拿证") to KeywordStyleMatch("mortarboard_cap", "ink_gold"),
    listOf("adoption", "pet birthday", "got a dog", "got a cat", "领养", "宠物生日", "接修猫", "接修狗") to KeywordStyleMatch("paw_heart", "paper_terracotta"),
    listOf("passport", "visa", "green card", "immigration", "护照", "签证", "绿卡", "换发护照") to KeywordStyleMatch("passport_book", "paper_indigo"),
    listOf("driver license", "driving test", "license passed", "learn to drive", "驾考", "考驾照", "科目一", "科目二", "科目三", "科目四", "拿到驾照") to KeywordStyleMatch("steering_wheel", "ink_gold"),
    listOf("tattoo", "inked", "fresh ink", "piercing", "纹身", "刺青", "穿刺", "文身") to KeywordStyleMatch("tattoo_machine", "paper_terracotta"),

    // Pillar D: Work, Career & Finance
    listOf("tax", "taxes", "tax return", "tax refund", "irs", "报税", "退税", "个税", "报税截止") to KeywordStyleMatch("tax_form", "paper_sage"),
    listOf("bonus", "dividend", "payday", "payout", "奖金", "分红", "年终奖", "发钱") to KeywordStyleMatch("bank_check", "ink_gold"),
    listOf("promotion", "promoted", "work anniversary", "new job", "raise", "升职", "加薪", "晋升", "入职周年", "新工作") to KeywordStyleMatch("briefcase_star", "ink_gold"),
    listOf("debt free", "paid off", "loan payoff", "debt payoff", "student loan", "还清", "还款结清", "房贷还清", "无债一身轻") to KeywordStyleMatch("receipt_cut", "sage_forest"),
    listOf("card freeze", "cut card", "credit freeze", "stop spending", "剪卡", "停卡", "冻结信用卡") to KeywordStyleMatch("card_cut", "ink_crimson"),
    listOf("launch", "ship", "release", "go live", "v1.0", "发布", "上线", "开售", "出货") to KeywordStyleMatch("shipping_crate", "ink_gold"),
    listOf("quarterly", "q1", "q2", "q3", "q4", "sprint review", "review meeting", "季度复盘", "季度总结", "季度述职") to KeywordStyleMatch("clipboard_check", "paper_sage"),

    // Pillar E: Social, Leisure & Life Goals
    listOf("date night", "anniversary dinner", "romantic dinner", "candlelight", "约会", "烛光晚餐", "二人世界") to KeywordStyleMatch("twin_candles", "paper_terracotta"),
    listOf("concert", "gig", "livehouse", "music festival", "festival", "演唱会", "现场", "音乐节", "livehouse") to KeywordStyleMatch("stage_mic", "ink_crimson"),
    listOf("movie night", "cinema", "film premiere", "movie marathon", "看电影", "观影", "电影之夜", "影院") to KeywordStyleMatch("clapperboard", "paper_terracotta"),
    listOf("road trip", "scenic drive", "rv trip", "vanlife", "自驾", "自驾游", "公路旅行", "房车") to KeywordStyleMatch("camper_van", "ink_gold"),
    listOf("fire", "retirement", "retire", "financial independence", "退休", "提前退休", "财务自由") to KeywordStyleMatch("beach_chair", "sage_forest"),

    // General & Foundation Categories
    listOf("haircut", "barber", "hair", "salon", "理发", "剪发", "剪头发", "头发") to KeywordStyleMatch("content_cut", ""),
    listOf("meditate", "meditation", "zen", "peace", "mind", "zazen", "冥想", "静坐", "禅") to KeywordStyleMatch("self_improvement", "sage_forest"),
    listOf("smoke", "cigar", "nicotine", "quit", "戒烟", "抽烟") to KeywordStyleMatch("smoke_free", "paper_terracotta"),
    listOf("oil", "car", "drive", "auto", "保养", "换机油", "汽车", "开车", "洗车") to KeywordStyleMatch("directions_car", "ink_gold"),
    listOf("read", "book", "study", "learn", "reading", "看书", "读书", "学习") to KeywordStyleMatch("book", ""),
    listOf("gym", "workout", "fitness", "lift", "exercise", "健身", "锻炼", "运动") to KeywordStyleMatch("fitness_center", "ink_crimson"),
    listOf("run", "running", "jog", "jogging", "marathon", "sprint", "sneaker", "跑步", "慢跑", "晨跑", "夜跑", "半马", "全马") to KeywordStyleMatch("sneaker", "ink_crimson"),
    listOf("walk", "step", "walking", "散步", "走步", "步数") to KeywordStyleMatch("directions_walk", "paper_terracotta"),
    listOf("water", "hydrate", "hydration", "drink", "喝水", "饮水") to KeywordStyleMatch("water_drop", "paper_indigo"),
    listOf("sleep", "bed", "rest", "insomnia", "睡眠", "早睡", "睡觉") to KeywordStyleMatch("bedtime", "sage_forest"),
    listOf("code", "program", "develop", "hack", "terminal", "写代码", "编程", "开发") to KeywordStyleMatch("terminal", "ink_gold"),
    listOf("salary", "rent", "bill", "invoice", "payroll", "finance", "发工资", "还贷", "房租", "账单") to KeywordStyleMatch("payments", "ink_gold"),
    listOf("coffee", "cafe", "espresso", "latte", "咖啡") to KeywordStyleMatch("local_cafe", "paper_terracotta"),
    listOf("beer", "bar", "alcohol", "drinking", "喝酒", "酒吧") to KeywordStyleMatch("local_bar", "ink_crimson"),
    listOf("tooth", "teeth", "dentist", "brush", "刷牙", "洗牙", "牙医", "拔牙", "补牙") to KeywordStyleMatch("tooth", "paper_sage"),
    listOf("pill", "vitamin", "supplement", "capsule", "维生素", "保健品", "维他命") to KeywordStyleMatch("pill", "paper_terracotta"),
    listOf("doctor", "hospital", "med", "health", "看病", "体检", "就医", "医院") to KeywordStyleMatch("local_hospital", "paper_indigo"),
    listOf("shower", "bath", "冲凉", "泡澡") to KeywordStyleMatch("shower", "paper_indigo"),
    listOf("weight", "weigh", "scale", "diet", "bmi", "fat", "称重", "体重", "减脂", "减肥") to KeywordStyleMatch("scales", "sage_forest"),
    listOf("fasting", "intermittent", "16:8", "fast", "轻断食", "断食", "辟谷") to KeywordStyleMatch("fasting_plate", "paper_sage"),
    listOf("heart", "cardio", "pulse", "bp", "blood pressure", "心率", "血压", "心跳", "有氧") to KeywordStyleMatch("heartbeat", "ink_crimson"),
    listOf("guitar", "instrument", "bass", "ukulele", "吉他", "弹琴", "尤克里里", "乐器") to KeywordStyleMatch("guitar", "paper_terracotta"),
    listOf("game", "gaming", "playstation", "xbox", "switch", "steam", "esport", "打游戏", "游戏", "开黑", "主机") to KeywordStyleMatch("game_controller", "ink_gold"),
    listOf("bike", "bicycle", "cycling", "cycle", "骑车", "自行车", "单车", "骑行", "公路车") to KeywordStyleMatch("bicycle", "paper_terracotta"),
    listOf("cook", "cooking", "meal", "prep", "chef", "homemade", "做饭", "下厨", "烧菜", "烹饪", "便当") to KeywordStyleMatch("cooking_pot", "paper_terracotta"),
    listOf("knife", "chop", "slice", "cutting", "切菜", "刀工", "配菜") to KeywordStyleMatch("knife", "paper_sage"),
    listOf("laundry", "wash clothes", "washing machine", "linen", "洗衣服", "洗衣", "床单", "晾衣") to KeywordStyleMatch("washing_machine", "paper_sage"),
    listOf("sweep", "dust", "broom", "vacuum", "chore", "大扫除", "扫地", "吸尘", "除螨", "整理房间") to KeywordStyleMatch("broom", "sage_forest"),
    listOf("knit", "knitting", "crochet", "yarn", "sew", "craft", "织毛衣", "毛线", "钩针", "编织", "缝纫") to KeywordStyleMatch("yarn", "sage_ochre"),
    listOf("wrench", "tool", "repair", "fix", "diy", "maintenance", "维修", "工具", "修车", "修理", "组装", "扳手") to KeywordStyleMatch("wrench", "ink_gold"),
    listOf("id card", "license", "pass", "certificate", "credential", "driver", "身份证", "证件", "考证", "证书") to KeywordStyleMatch("identification_card", ""),
    listOf("save", "saving", "piggy", "invest", "budget", "deposit", "存钱", "攒钱", "储蓄", "理财", "定投", "记账") to KeywordStyleMatch("piggy_bank", "ink_gold"),
    listOf("newspaper", "article", "journal", "read news", "新闻", "报纸", "看报", "读报", "时事") to KeywordStyleMatch("newspaper", "paper_sage"),
    listOf("target", "okr", "kpi", "bullseye", "目标", "指标", "靶心", "定标") to KeywordStyleMatch("target", "ink_crimson"),
    listOf("key", "house key", "apartment", "lock", "钥匙", "门锁", "挂锁") to KeywordStyleMatch("key", "ink_gold"),
    listOf("ticket", "boarding", "门票", "车票", "入场券") to KeywordStyleMatch("ticket", "paper_terracotta"),
    listOf("stroller", "pram", "baby walk", "carriage", "推车", "遛娃", "婴儿车", "带娃") to KeywordStyleMatch("baby_carriage", "paper_terracotta"),
    listOf("medal", "marathon finisher", "competition", "奖牌", "完赛", "金牌", "名次") to KeywordStyleMatch("medal", "ink_gold"),
    listOf("crown", "coronation", "royalty", "queen", "glory", "皇冠", "冠军", "荣耀") to KeywordStyleMatch("crown", "ink_gold"),
    listOf("deal", "contract", "partner", "handshake", "agreement", "签约", "合作", "合同", "握手", "谈成") to KeywordStyleMatch("handshake", "sage_forest"),
    listOf("gift", "present", "surprise", "box", "礼物", "送礼", "收礼", "惊喜", "礼盒") to KeywordStyleMatch("gift", "paper_terracotta"),
    listOf("mail", "letter", "envelope", "postcard", "信件", "写信", "寄信", "明信片", "邮件") to KeywordStyleMatch("envelope", ""),
    listOf("mountain", "hike", "hiking", "climb", "summit", "peak", "爬山", "登山", "徒步", "登顶", "山峰") to KeywordStyleMatch("mountains", "sage_forest"),
    listOf("campfire", "bonfire", "fire", "cozy", "篝火", "营火", "烤火", "围炉") to KeywordStyleMatch("campfire", "paper_terracotta"),
    listOf("camp", "camping", "tent", "backpacking", "露营", "搭帐篷", "野营", "帐篷") to KeywordStyleMatch("tent", "sage_forest"),
    listOf("compass", "explore", "adventure", "orient", "orientation", "指南针", "罗盘", "探险", "定向") to KeywordStyleMatch("compass", "ink_gold"),
    listOf("photo", "photography", "camera", "shoot", "pic", "snap", "拍照", "摄影", "相机", "相片", "留念") to KeywordStyleMatch("camera", "paper_terracotta"),
    listOf("plant", "flower", "garden", "tree", "flora", "植物", "花园") to KeywordStyleMatch("local_florist", "sage_forest"),
    listOf("food", "dine", "dining", "eating", "restaurant", "轻食", "下馆子", "外卖", "餐厅") to KeywordStyleMatch("restaurant", "paper_terracotta"),
    listOf("wedding", "marriage", "engagement", "marry", "proposal", "bride", "groom", "结婚", "订婚", "婚礼", "求婚") to KeywordStyleMatch("wedding_ring", "paper_terracotta"),
    listOf("toast", "champagne", "cheers", "celebrate", "celebration", "干杯", "举杯", "香槟") to KeywordStyleMatch("champagne_flutes", "ink_gold"),
    listOf("detox", "screen time", "screen-free", "digital detox", "unplug", "phone free", "戒手机", "控屏", "屏幕时间", "远离手机") to KeywordStyleMatch("phone_slash", "paper_indigo"),
    listOf("wellness", "sprout", "digital wellness", "mindful", "护眼", "数字健康") to KeywordStyleMatch("sprout_device", "sage_forest"),
    listOf("hourglass", "sprint", "沙漏") to KeywordStyleMatch("hourglass_spark", "sage_forest"),
    listOf("music", "piano", "song", "sing", "listening", "听歌", "音乐", "歌曲", "唱歌") to KeywordStyleMatch("music_note", "paper_terracotta"),
    listOf("dog", "cat", "pet", "vet", "puppy", "kitten", "宠物", "猫", "狗") to KeywordStyleMatch("pets", "paper_terracotta"),
    listOf("travel", "trip", "flight", "vacation", "fly", "plane", "旅游", "出差", "旅行", "度假") to KeywordStyleMatch("flight", "paper_indigo"),
    listOf("sun", "morning", "wake", "sunrise", "早起", "日出", "太阳") to KeywordStyleMatch("sunny", "ink_gold"),
    listOf("baby", "child", "kid", "born", "birth", "宝宝", "出生", "孩子") to KeywordStyleMatch("child_care", "paper_terracotta"),
    listOf("cake", "birthday", "anniversary", "party", "celebrate", "celebration", "生日", "纪念日", "周年", "庆祝") to KeywordStyleMatch("cake", "paper_terracotta"),
    listOf("clean", "tidy", "house", "home", "room", "打扫", "家务") to KeywordStyleMatch("home", "sage_forest"),
    listOf("sail", "boat", "sea", "ocean", "航海", "出海") to KeywordStyleMatch("sailing", "paper_indigo"),
    listOf("eco", "nature", "green", "recycle", "环保", "绿色") to KeywordStyleMatch("eco", "sage_forest"),
    listOf("award", "win", "trophy", "goal", "milestone", "获奖", "奖项", "成就") to KeywordStyleMatch("emoji_events", "ink_gold"),
)

private class CompiledKeywordRule(
    val regex: Regex?,
    val substrings: List<String>,
    val match: KeywordStyleMatch,
)

private val COMPILED_KEYWORD_RULES: List<CompiledKeywordRule> by lazy {
    KEYWORD_STYLE_RULES.map { (keywords, match) ->
        val (ascii, nonAscii) = keywords.partition { kw ->
            kw.firstOrNull()?.let { it in 'a'..'z' || it in '0'..'9' } == true && kw.all { it.code < 128 }
        }
        val regex = if (ascii.isNotEmpty()) {
            Regex("""\b(?:${ascii.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})\b""")
        } else null
        CompiledKeywordRule(regex, nonAscii, match)
    }
}

/**
 * Resolves a keyword style match from the item [name], returning null if no match found.
 */
fun matchKeywordStyle(name: String): KeywordStyleMatch? {
    if (name.isBlank()) return null
    val clean = name.trim().lowercase()
    for (rule in COMPILED_KEYWORD_RULES) {
        if (rule.regex?.containsMatchIn(clean) == true || rule.substrings.any { clean.contains(it) }) {
            return rule.match
        }
    }
    return null
}

/** Maps a stored icon name to its embedded vector drawable resource. */
@DrawableRes
fun iconRes(name: String): Int = ICON_MAP[name]?.drawableRes ?: R.drawable.ic_person

/** Maps a stored icon name to its human-friendly TalkBack description string resource. */
@StringRes
fun iconDescriptionRes(name: String): Int = ICON_MAP[name]?.descriptionRes ?: R.string.cd_icon_person

