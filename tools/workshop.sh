#!/usr/bin/env bash
# Assemble ~/Zomboid/Workshop/ClearedRooms for the in-game Workshop uploader (run tools/build.sh first).
set -euo pipefail
R=$(cd "$(dirname "$0")/.." && pwd)
W=${1:-$HOME/Zomboid/Workshop}/ClearedRooms
[ -f "$R/mod/ClearedRooms/42/media/java/ClearedRooms.jar" ] || { echo "build first: tools/build.sh" >&2; exit 1; }
mkdir -p "$W/Contents/mods/ClearedRooms"
rsync -a --delete "$R/mod/ClearedRooms/" "$W/Contents/mods/ClearedRooms/"
cp "$R/workshop/preview.png" "$W/preview.png"
# keep the id line the uploader adds after the first upload
if [ -f "$W/workshop.txt" ] && grep -q '^id=' "$W/workshop.txt"; then
  { cat "$R/workshop/workshop.txt"; grep '^id=' "$W/workshop.txt"; } > "$W/workshop.txt.new" && mv "$W/workshop.txt.new" "$W/workshop.txt"
else
  cp "$R/workshop/workshop.txt" "$W/workshop.txt"
fi
echo "workshop item ready at $W"
