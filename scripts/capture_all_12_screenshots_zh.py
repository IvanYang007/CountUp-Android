import subprocess
import time
import os
import shutil

ADB = r"D:\Android\Sdk\platform-tools\adb.exe"
OUTPUT_DIRS = ["playstore_package", "playstore_package/zh", "artifacts"]

for d in OUTPUT_DIRS:
    os.makedirs(d, exist_ok=True)

def adb(cmd):
    if isinstance(cmd, str):
        return subprocess.run(f'"{ADB}" {cmd}', shell=True, capture_output=True, text=True)
    else:
        return subprocess.run([ADB] + cmd, capture_output=True, text=True)

def tap(x, y):
    adb(f"shell input tap {x} {y}")
    time.sleep(0.5)

def swipe(x1, y1, x2, y2, duration=300):
    adb(f"shell input swipe {x1} {y1} {x2} {y2} {duration}")
    time.sleep(0.5)

def key(code):
    adb(f"shell input keyevent {code}")
    time.sleep(0.4)

def set_night(enable=False):
    adb(f'shell "cmd uimode night {"yes" if enable else "no"}"')
    time.sleep(0.6)

def open_main():
    adb('shell "am start -n com.countup.app/.MainActivity"')
    time.sleep(0.8)

def open_item(item_id):
    adb(f'shell "am start -n com.countup.app/.MainActivity --es EXTRA_TARGET_ITEM_ID {item_id}"')
    time.sleep(0.7)

def capture(filename):
    remote = f"/sdcard/{filename}"
    adb(f"shell screencap -p {remote}")
    for d in OUTPUT_DIRS:
        local = os.path.join(d, filename)
        subprocess.run([ADB, "pull", remote, local], check=True, capture_output=True)
    print(f"Captured: {filename}")

print("Starting Chinese 12-screenshot capture sequence...")

# Ensure Chinese cards are injected
subprocess.run(["python", "scripts/populate_six_cards_zh.py"], check=True)
time.sleep(1.2)

# -------------------------------------------------------------
# Screenshot 1: Main Cards List (Light Mode)
# -------------------------------------------------------------
print("1. Capturing Main Cards List (Light Mode)...")
set_night(False)
open_main()
# Scroll to top
swipe(500, 800, 500, 1800, 200)
time.sleep(0.5)
capture("screenshot_01_main_cards_light.png")

# -------------------------------------------------------------
# Screenshot 2: Main Cards List (Dark Mode)
# -------------------------------------------------------------
print("2. Capturing Main Cards List (Dark Mode)...")
set_night(True)
open_main()
swipe(500, 800, 500, 1800, 200)
time.sleep(0.5)
capture("screenshot_02_main_cards_dark.png")

# -------------------------------------------------------------
# Screenshot 3: Future Countdown & Scrolled List (Light Mode)
# -------------------------------------------------------------
print("3. Capturing Future Countdown & Scrolled List (Light Mode)...")
set_night(False)
open_main()
swipe(500, 1800, 500, 950, 300)
time.sleep(0.5)
capture("screenshot_03_future_countdown_light.png")

# -------------------------------------------------------------
# Screenshot 4: Edit Item Dialog (Light Mode)
# -------------------------------------------------------------
print("4. Capturing Edit Item Dialog (Light Mode)...")
open_main()
swipe(500, 800, 500, 1800, 200)
time.sleep(0.4)
open_item("card-sobriety-100d")
time.sleep(0.6)
capture("screenshot_04_edit_item_dialog_light.png")

# -------------------------------------------------------------
# Screenshot 5: Tactile Icon Picker Grid (Light Mode)
# -------------------------------------------------------------
print("5. Capturing Tactile Icon Picker Grid (Light Mode)...")
# Scroll inside dialog to show the tactile icon grid
swipe(500, 1600, 500, 950, 300)
time.sleep(0.5)
capture("screenshot_05_tactile_icon_picker_light.png")
key("KEYCODE_BACK") # Close editor
time.sleep(0.4)

# -------------------------------------------------------------
# Screenshot 6: Solar Calendar Date Picker (Light Mode)
# -------------------------------------------------------------
print("6. Capturing Solar Calendar Date Picker (Light Mode)...")
open_main()
open_item("card-sobriety-100d")
time.sleep(0.5)
# Tap Anchor date chip (2026年6月3日)
tap(680, 1050)
time.sleep(0.6)
capture("screenshot_06_solar_calendar_picker_light.png")
key("KEYCODE_BACK") # Close calendar picker
time.sleep(0.4)
key("KEYCODE_BACK") # Close editor
time.sleep(0.4)

# -------------------------------------------------------------
# Screenshot 7: Sort Order & Search Popover (Dark Mode)
# -------------------------------------------------------------
print("7. Capturing Sort Order & Search Popover (Dark Mode)...")
set_night(True)
open_main()
swipe(500, 800, 500, 1800, 200)
time.sleep(0.4)
# Tap Sort popover
tap(710, 410)
time.sleep(0.6)
capture("screenshot_07_sort_search_filter_dark.png")
key("KEYCODE_BACK") # Close popover
time.sleep(0.4)

# -------------------------------------------------------------
# Screenshot 8: Poetic Background Theme Cycling (Light Mode)
# -------------------------------------------------------------
print("8. Capturing Background Theme Cycling (Light Mode)...")
set_night(False)
open_main()
# Cycle theme via Ensō avatar tap
tap(130, 260)
time.sleep(0.4)
capture("screenshot_08_theme_cycling_light.png")

# -------------------------------------------------------------
# Screenshot 9: Edit Item Dialog (Dark Mode)
# -------------------------------------------------------------
print("9. Capturing Edit Item Dialog (Dark Mode)...")
set_night(True)
open_main()
swipe(500, 800, 500, 1800, 200)
time.sleep(0.4)
open_item("card-coldplunge-45d")
time.sleep(0.6)
capture("screenshot_09_edit_item_dialog_dark.png")
key("KEYCODE_BACK") # Close editor
time.sleep(0.4)

# -------------------------------------------------------------
# Screenshot 10: Data & Backup Privacy Dialog (Light Mode)
# -------------------------------------------------------------
print("10. Capturing Data & Backup Privacy Dialog (Light Mode)...")
set_night(False)
open_main()
swipe(500, 800, 500, 1800, 200)
time.sleep(0.4)
# Tap settings gear icon at top right of subheader
tap(985, 435)
time.sleep(0.6)
capture("screenshot_10_privacy_backup_dialog_light.png")
key("KEYCODE_BACK") # Close settings
time.sleep(0.4)

# -------------------------------------------------------------
# Screenshot 11: Home Screen Widgets - Zen Horizon & Pebble (Dark Mode)
# -------------------------------------------------------------
print("11. Capturing Home Screen Widgets - Horizon & Pebble (Dark Mode)...")
set_night(True)
# Bring main to front briefly to ensure RemoteViews update
open_main()
time.sleep(0.5)
key("KEYCODE_HOME")
time.sleep(0.6)
# Ensure we are on page 1 (swipe right)
swipe(100, 1200, 900, 1200, 300)
time.sleep(0.6)
capture("screenshot_11_widgets_horizon_dark.png")

# -------------------------------------------------------------
# Screenshot 12: Home Screen Widgets - Multi-Item & Solar Rhythm (Light Mode)
# -------------------------------------------------------------
print("12. Capturing Home Screen Widgets - Multi-Item & Solar Rhythm (Light Mode)...")
set_night(False)
open_main()
time.sleep(0.5)
key("KEYCODE_HOME")
time.sleep(0.6)
# Swipe left to page 2
swipe(900, 1200, 100, 1200, 300)
time.sleep(0.6)
capture("screenshot_12_widgets_solar_multi_light.png")

print("All 12 Chinese screenshots captured successfully!")
