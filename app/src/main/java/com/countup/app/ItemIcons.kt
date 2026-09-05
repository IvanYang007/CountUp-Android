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
)

val HEALTH_ICONS: List<String> = HEALTH_ICON_ENTRIES.map { it.name }
val HABIT_ICONS: List<String> = HABIT_ICON_ENTRIES.map { it.name }
val WORK_ICONS: List<String> = WORK_ICON_ENTRIES.map { it.name }
val MILESTONE_ICONS: List<String> = MILESTONE_ICON_ENTRIES.map { it.name }
val SOCIAL_ICONS: List<String> = SOCIAL_ICON_ENTRIES.map { it.name }
val NATURE_ICONS: List<String> = NATURE_ICON_ENTRIES.map { it.name }

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
val KEYWORD_STYLE_RULES: List<Pair<List<String>, KeywordStyleMatch>> = listOf(
    listOf("haircut", "barber", "hair", "salon", "理发", "剪发", "剪头发", "头发") to KeywordStyleMatch("content_cut", ""),
    listOf("meditat", "zen", "peace", "mind", "zazen", "冥想", "静坐", "禅") to KeywordStyleMatch("self_improvement", "sage_forest"),
    listOf("smoke", "cigar", "nicotine", "quit", "戒烟", "抽烟") to KeywordStyleMatch("smoke_free", "paper_terracotta"),
    listOf("oil", "car", "drive", "auto", "保养", "换机油", "汽车", "开车", "洗车") to KeywordStyleMatch("directions_car", "ink_gold"),
    listOf("read", "book", "study", "learn", "reading", "看书", "读书", "学习") to KeywordStyleMatch("book", ""),
    listOf("gym", "workout", "fitness", "lift", "exercise", "健身", "锻炼", "运动") to KeywordStyleMatch("fitness_center", "ink_crimson"),
    listOf("run", "running", "jog", "jogging", "marathon", "sprint", "sneaker", "跑步", "慢跑", "晨跑", "夜跑", "半马", "全马") to KeywordStyleMatch("sneaker", "ink_crimson"),
    listOf("walk", "step", "walking", "散步", "走步", "步数") to KeywordStyleMatch("directions_walk", "paper_terracotta"),
    listOf("water", "hydrat", "drink", "hydrate", "喝水", "饮水") to KeywordStyleMatch("water_drop", "paper_indigo"),
    listOf("sleep", "bed", "rest", "insomnia", "睡眠", "早睡", "睡觉") to KeywordStyleMatch("bedtime", "sage_forest"),
    listOf("code", "program", "develop", "hack", "terminal", "写代码", "编程", "开发") to KeywordStyleMatch("terminal", "ink_gold"),
    listOf("salary", "rent", "bill", "invoice", "payroll", "finance", "发工资", "还贷", "房租", "账单") to KeywordStyleMatch("payments", "ink_gold"),
    listOf("coffee", "cafe", "espresso", "latte", "咖啡") to KeywordStyleMatch("local_cafe", "paper_terracotta"),
    listOf("beer", "bar", "alcohol", "sober", "sobriety", "戒酒", "喝酒", "酒吧") to KeywordStyleMatch("local_bar", "ink_crimson"),
    listOf("tooth", "teeth", "dental", "dentist", "floss", "brush", "刷牙", "洗牙", "牙医", "拔牙", "补牙") to KeywordStyleMatch("tooth", "paper_sage"),
    listOf("pill", "vitamin", "supplement", "capsule", "吃药", "维生素", "保健品", "维他命") to KeywordStyleMatch("pill", "paper_terracotta"),
    listOf("doctor", "hospital", "med", "health", "看病", "体检", "就医", "医院") to KeywordStyleMatch("local_hospital", "paper_indigo"),
    listOf("shower", "cold shower", "bath", "skincare", "洗澡", "冲凉", "冷水澡", "泡澡", "护肤") to KeywordStyleMatch("shower", "paper_indigo"),
    listOf("weight", "weigh", "scale", "diet", "bmi", "fat", "称重", "体重", "减脂", "减肥", "控糖") to KeywordStyleMatch("scales", "sage_forest"),
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
    listOf("id card", "license", "pass", "certificate", "credential", "driver", "驾照", "身份证", "证件", "护照", "考证", "证书") to KeywordStyleMatch("identification_card", ""),
    listOf("save", "saving", "piggy", "invest", "budget", "deposit", "存钱", "攒钱", "储蓄", "理财", "定投", "记账") to KeywordStyleMatch("piggy_bank", "ink_gold"),
    listOf("newspaper", "article", "journal", "read news", "新闻", "报纸", "看报", "读报", "时事") to KeywordStyleMatch("newspaper", "paper_sage"),
    listOf("target", "okr", "kpi", "bullseye", "目标", "指标", "靶心", "定标") to KeywordStyleMatch("target", "ink_crimson"),
    listOf("key", "house key", "new home", "apartment", "lock", "钥匙", "交房", "新房", "提车", "入住") to KeywordStyleMatch("key", "ink_gold"),
    listOf("ticket", "concert", "movie", "cinema", "show", "boarding", "门票", "演唱会", "电影", "演出", "看戏", "机票") to KeywordStyleMatch("ticket", "paper_terracotta"),
    listOf("stroller", "pram", "baby walk", "carriage", "推车", "遛娃", "婴儿车", "带娃") to KeywordStyleMatch("baby_carriage", "paper_terracotta"),
    listOf("medal", "marathon finisher", "competition", "奖牌", "完赛", "金牌", "名次") to KeywordStyleMatch("medal", "ink_gold"),
    listOf("crown", "coronation", "royalty", "queen", "glory", "皇冠", "冠军", "荣耀") to KeywordStyleMatch("crown", "ink_gold"),
    listOf("deal", "contract", "partner", "handshake", "agreement", "签约", "合作", "合同", "握手", "谈成") to KeywordStyleMatch("handshake", "sage_forest"),
    listOf("gift", "present", "surprise", "box", "礼物", "送礼", "收礼", "惊喜", "礼盒") to KeywordStyleMatch("gift", "paper_terracotta"),
    listOf("mail", "letter", "envelope", "postcard", "信件", "写信", "寄信", "明信片", "邮件") to KeywordStyleMatch("envelope", ""),
    listOf("mountain", "hike", "hiking", "climb", "summit", "peak", "爬山", "登山", "徒步", "登顶", "山峰") to KeywordStyleMatch("mountains", "sage_forest"),
    listOf("campfire", "bonfire", "fire", "cozy", "篝火", "营火", "烤火", "围炉") to KeywordStyleMatch("campfire", "paper_terracotta"),
    listOf("camp", "camping", "tent", "backpacking", "露营", "搭帐篷", "野营", "帐篷") to KeywordStyleMatch("tent", "sage_forest"),
    listOf("compass", "explore", "adventure", "orient", "指南针", "罗盘", "探险", "定向") to KeywordStyleMatch("compass", "ink_gold"),
    listOf("photo", "photography", "camera", "shoot", "pic", "snap", "拍照", "摄影", "相机", "相片", "留念") to KeywordStyleMatch("camera", "paper_terracotta"),
    listOf("plant", "flower", "garden", "tree", "flora", "浇花", "植物", "花园") to KeywordStyleMatch("local_florist", "sage_forest"),
    listOf("food", "dine", "dining", "eating", "restaurant", "fasting", "轻食", "断食", "下馆子", "外卖", "餐厅") to KeywordStyleMatch("restaurant", "paper_terracotta"),
    listOf("music", "piano", "song", "sing", "listening", "听歌", "音乐", "歌曲", "唱歌") to KeywordStyleMatch("music_note", "paper_terracotta"),
    listOf("dog", "cat", "pet", "vet", "puppy", "kitten", "宠物", "猫", "狗") to KeywordStyleMatch("pets", "paper_terracotta"),
    listOf("travel", "trip", "flight", "vacation", "fly", "plane", "旅游", "出差", "旅行", "度假") to KeywordStyleMatch("flight", "paper_indigo"),
    listOf("sun", "morning", "wake", "sunrise", "早起", "日出", "太阳") to KeywordStyleMatch("sunny", "ink_gold"),
    listOf("baby", "child", "kid", "born", "birth", "宝宝", "出生", "孩子") to KeywordStyleMatch("child_care", "paper_terracotta"),
    listOf("cake", "birthday", "anniversary", "party", "celebrat", "生日", "纪念日", "周年", "庆祝") to KeywordStyleMatch("cake", "paper_terracotta"),
    listOf("clean", "tidy", "house", "home", "room", "打扫", "家务") to KeywordStyleMatch("home", "sage_forest"),
    listOf("sail", "boat", "sea", "ocean", "航海", "出海") to KeywordStyleMatch("sailing", "paper_indigo"),
    listOf("eco", "nature", "green", "recycle", "环保", "绿色") to KeywordStyleMatch("eco", "sage_forest"),
    listOf("award", "win", "trophy", "goal", "milestone", "获奖", "奖项", "成就") to KeywordStyleMatch("emoji_events", "ink_gold"),
)

/**
 * Resolves a keyword style match from the item [name], returning null if no match found.
 */
fun matchKeywordStyle(name: String): KeywordStyleMatch? {
    if (name.isBlank()) return null
    val clean = name.trim().lowercase()
    for ((keywords, match) in KEYWORD_STYLE_RULES) {
        if (keywords.any { clean.contains(it) }) {
            return match
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

