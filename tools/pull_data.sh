#!/usr/bin/env bash
DEVICE="R3CX705W62D"
REMOTE="/sdcard/Documents/Minseo5/data.json"
LOCAL="$(dirname "$0")/data.json"

MSYS_NO_PATHCONV=1 adb -s "$DEVICE" pull "$REMOTE" "$LOCAL"
echo "저장: $LOCAL"
