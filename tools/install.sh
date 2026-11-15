#!/usr/bin/env bash
# Copy the built mod into ~/Zomboid/mods/ClearedRooms (run tools/build.sh first).
set -euo pipefail
R=$(cd "$(dirname "$0")/.." && pwd)
DEST=${1:-$HOME/Zomboid/mods}/ClearedRooms
[ -f "$R/mod/ClearedRooms/42/media/java/ClearedRooms.jar" ] || { echo "build first: tools/build.sh" >&2; exit 1; }
mkdir -p "$DEST"; rsync -a --delete "$R/mod/ClearedRooms/" "$DEST/"
echo "installed to $DEST; enable \"Cleared Rooms Stay Clear\" after ZombieBuddy"
