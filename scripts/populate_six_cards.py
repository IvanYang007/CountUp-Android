import json
import time
import subprocess
import os

ADB = r"D:\Android\Sdk\platform-tools\adb.exe"

# Current date on emulator is 2026-09-11 -> epochDay = 20707
TODAY_EPOCH_DAY = 20707

items = [
    {
        "id": "card-sobriety-100d",
        "name": "Sobriety Streak",
        "epochDay": TODAY_EPOCH_DAY - 100,  # 20607 (100 days)
        "comment": "Living clear, day by day",
        "icon": "sober_glass_inverted",
        "cardColor": "paper_terracotta",
        "futureFlag": False,
        "showInWidget": True,
        "resetCount": 0,
        "totalResetDays": 0
    },
    {
        "id": "card-meditation-170d",
        "name": "Meditation & Zen Breathing",
        "epochDay": TODAY_EPOCH_DAY - 170,  # 20537 (170 days)
        "comment": "Morning 20-min seated mindfulness",
        "icon": "zen_bell",
        "cardColor": "paper_sage",
        "futureFlag": False,
        "showInWidget": True,
        "resetCount": 1,
        "totalResetDays": 30
    },
    {
        "id": "card-reading-85d",
        "name": "Deep Reading: Poetry",
        "epochDay": TODAY_EPOCH_DAY - 85,   # 20622 (85 days)
        "comment": "Tang & Song dynasty anthologies",
        "icon": "reading_beacon",
        "cardColor": "paper_indigo",
        "futureFlag": False,
        "showInWidget": True,
        "resetCount": 0,
        "totalResetDays": 0
    },
    {
        "id": "card-coldplunge-45d",
        "name": "Cold Water Plunge",
        "epochDay": TODAY_EPOCH_DAY - 45,   # 20662 (45 days)
        "comment": "Morning vitality & discipline",
        "icon": "cold_tub",
        "cardColor": "sage_forest",
        "futureFlag": False,
        "showInWidget": True,
        "resetCount": 2,
        "totalResetDays": 14
    },
    {
        "id": "card-kyoto-future-15d",
        "name": "Kyoto Zen Journey",
        "epochDay": TODAY_EPOCH_DAY + 15,   # 20722 (-15 days future countdown)
        "comment": "Autumn temple & rock garden walk",
        "icon": "mountain_pass",
        "cardColor": "ink_gold",
        "futureFlag": True,
        "showInWidget": True,
        "resetCount": 0,
        "totalResetDays": 0
    },
    {
        "id": "card-haircut-25d",
        "name": "Last Haircut",
        "epochDay": TODAY_EPOCH_DAY - 25,   # 20682 (25 days)
        "comment": "Regular maintenance trim",
        "icon": "hair_tint_brush",
        "cardColor": "",
        "futureFlag": False,
        "showInWidget": True,
        "resetCount": 3,
        "totalResetDays": 75
    }
]

items_json_str = json.dumps(items)
revision = int(time.time() * 1000)

# Build XML for SharedPreferences
xml_content = f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="theme_mode">system</string>
    <string name="zen_horizon_binding_74">card-sobriety-100d</string>
    <string name="solar_rhythm_binding_78">card-kyoto-future-15d</string>
    <string name="zen_pebble_binding_72">card-meditation-170d</string>
    <string name="hero_widget_binding_73">card-reading-85d</string>
    <long name="storage_revision_v1" value="{revision}" />
    <string name="sort_order_v1">days_desc</string>
    <string name="items_v1">{items_json_str.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')}</string>
    <boolean name="migrated_v1" value="true" />
</map>
"""

os.makedirs("artifacts", exist_ok=True)
with open("artifacts/countup_prefs.xml", "w", encoding="utf-8") as f:
    f.write(xml_content)

with open("artifacts/countup_backup.json", "w", encoding="utf-8") as f:
    f.write(items_json_str)

with open("artifacts/countup_backup.rev", "w", encoding="utf-8") as f:
    f.write(str(revision))

print(f"Generated 6 cards with revision {revision}.")

# Push to device tmp
subprocess.run([ADB, "push", "artifacts/countup_prefs.xml", "/data/local/tmp/countup_prefs.xml"], check=True)
subprocess.run([ADB, "push", "artifacts/countup_backup.json", "/data/local/tmp/countup_backup.json"], check=True)
subprocess.run([ADB, "push", "artifacts/countup_backup.rev", "/data/local/tmp/countup_backup.rev"], check=True)

# Copy via run-as tee
cmd = (
    "run-as com.countup.app mkdir -p shared_prefs files && "
    "cat /data/local/tmp/countup_prefs.xml | run-as com.countup.app tee shared_prefs/countup_prefs.xml > /dev/null && "
    "cat /data/local/tmp/countup_backup.json | run-as com.countup.app tee files/countup_backup.json > /dev/null && "
    "cat /data/local/tmp/countup_backup.rev | run-as com.countup.app tee files/countup_backup.rev > /dev/null && "
    "run-as com.countup.app chmod 660 shared_prefs/countup_prefs.xml files/countup_backup.json files/countup_backup.rev"
)
subprocess.run([ADB, "shell", cmd], check=True)
print("Successfully copied prefs and backup to com.countup.app private data dir.")

# Force stop and restart app
subprocess.run([ADB, "shell", "am", "force-stop", "com.countup.app"], check=True)
subprocess.run([ADB, "shell", "am", "start", "-n", "com.countup.app/.MainActivity"], check=True)
print("Restarted com.countup.app MainActivity.")
