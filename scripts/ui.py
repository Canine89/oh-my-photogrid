#!/usr/bin/env python3
"""Tiny UI driver over adb + uiautomator for local smoke tests.
Usage: ui.py tap-text <text> | tap-exact <text> | tap-desc <desc> | tap-id <res-id-suffix> | dump | tap x y | swipe x1 y1 x2 y2 ms"""
import os, re, subprocess, sys, time
ADB = os.path.join(os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools"), "platform-tools", "adb")

def sh(*a):
    return subprocess.run([ADB, "shell", *a], capture_output=True, text=True).stdout

def dump():
    sh("uiautomator", "dump", "/sdcard/ui.xml")
    return subprocess.run([ADB, "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout

def nodes(xml):
    for m in re.finditer(r"<node [^>]*>", xml):
        n = m.group(0)
        attrs = dict(re.findall(r'([\w-]+)="([^"]*)"', n))
        b = re.findall(r"\d+", attrs.get("bounds", ""))
        if len(b) == 4:
            attrs["center"] = ((int(b[0]) + int(b[2])) // 2, (int(b[1]) + int(b[3])) // 2)
            yield attrs

def find(key, value, contains=True, retries=10):
    for _ in range(retries):
        for a in nodes(dump()):
            v = a.get(key, "")
            if (value in v) if contains else (v == value):
                return a
        time.sleep(0.7)
    raise SystemExit(f"not found: {key}={value}")

cmd = sys.argv[1]
if cmd == "dump":
    for a in nodes(dump()):
        label = a.get("text") or a.get("content-desc") or a.get("resource-id")
        if label:
            print(a["center"], repr(label)[:80])
elif cmd == "tap-scroll":
    # Scroll down (swipe up) until the text is visible, then tap it.
    for _ in range(6):
        hit = next((a for a in nodes(dump()) if sys.argv[2] in a.get("text", "")), None)
        if hit:
            sh("input", "tap", *map(str, hit["center"]))
            print("tapped", hit["center"])
            break
        w, h = map(int, re.findall(r"\d+", sh("wm", "size").splitlines()[-1])[-2:])
        sh("input", "swipe", str(w // 2), str(int(h * 0.75)), str(w // 2), str(int(h * 0.35)), "300")
        time.sleep(0.8)
    else:
        raise SystemExit(f"not found after scrolling: {sys.argv[2]}")
elif cmd == "tap-exact":
    a = find("text", sys.argv[2], contains=False)
    sh("input", "tap", *map(str, a["center"]))
    print("tapped", a["center"])
elif cmd in ("tap-text", "tap-desc", "tap-id"):
    key = {"tap-text": "text", "tap-desc": "content-desc", "tap-id": "resource-id"}[cmd]
    a = find(key, sys.argv[2])
    sh("input", "tap", *map(str, a["center"]))
    print("tapped", a["center"])
elif cmd == "tap":
    sh("input", "tap", sys.argv[2], sys.argv[3])
elif cmd == "swipe":
    sh("input", "swipe", *sys.argv[2:7])
