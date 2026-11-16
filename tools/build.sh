#!/usr/bin/env bash
# Build mod/ClearedRooms/42/media/java/ClearedRooms.jar.
# Usage: tools/build.sh [--game DIR]   (DIR holds projectzomboid.jar and ZombieBuddy.jar)
set -euo pipefail
R=$(cd "$(dirname "$0")/.." && pwd)
GAME=${PZ_GAME_DIR:-$HOME/.local/share/Steam/steamapps/common/ProjectZomboid/projectzomboid}
[ "${1:-}" = --game ] && GAME=$2
[ -f "$GAME/projectzomboid.jar" ] && [ -f "$GAME/ZombieBuddy.jar" ] || { echo "need projectzomboid.jar and ZombieBuddy.jar in $GAME (--game DIR)" >&2; exit 1; }
CP="$GAME/projectzomboid.jar:$GAME/ZombieBuddy.jar"
B=$R/build; rm -rf "$B"; mkdir -p "$B/classes"
javac --release 25 -Xlint:all -d "$B/classes" -cp "$CP" $(find "$R/src" -name '*.java')
JAR=$R/mod/ClearedRooms/42/media/java/ClearedRooms.jar
printf 'Manifest-Version: 1.0\nImplementation-Title: Cleared Rooms Stay Clear\n' > "$B/MANIFEST.MF"
jar cfm "$JAR" "$B/MANIFEST.MF" -C "$B/classes" .
# ZombieBuddy only applies @Patch classes in exactly the javaPkgName package.
BAD=$(cd "$B/classes" && for c in $(find . -name '*.class'); do
  javap -v "$c" 2>/dev/null | grep -q 'me\.zed_0xff\.zombie_buddy\.Patch' && echo "$c"; done | grep -v '^\./clearedrooms/[^/]*\.class$' || true)
[ -z "$BAD" ] || { echo "error: @Patch classes outside package clearedrooms: $BAD" >&2; exit 1; }
# The patched method must exist with this exact signature.
javap -p -cp "$GAME/projectzomboid.jar" zombie.iso.IsoChunk | grep -q 'public boolean isSpawnedRoom(long);' \
  || { echo "error: IsoChunk.isSpawnedRoom(long) not found in this game version" >&2; exit 1; }
# The other patched methods too.
P=$(javap -p -cp "$GAME/projectzomboid.jar" zombie.popman.ZombiePopulationManager zombie.MapCollisionData)
for sig in 'private void addZombieStanding(float, float, float, zombie.iso.IsoDirections, int, zombie.popman.ZombieStateFlags, int);' \
           'private void addZombieMoving(float, float, float, zombie.iso.IsoDirections, int, zombie.popman.ZombieStateFlags, int, int, int);' \
           'public void removeChunkFromWorld(zombie.iso.IsoChunk);' \
           'public void virtualizeZombie(zombie.characters.IsoZombie);' \
           'public void save();' 'public void stop();'; do
  grep -qF "$sig" <<<"$P" || { echo "error: '$sig' not found in this game version" >&2; exit 1; }
done
echo "built $JAR ($(sha256sum "$JAR" | cut -c1-16))"
