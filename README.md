# Cleared Rooms Stay Clear

A Project Zomboid Build 42.22 fix (ZombieBuddy Java mod): houses you cleared, boarded up or lived in
no longer fill up with zombies again.

## The bug

When a chunk loads, the game fills each building room with zombies unless the room is marked
`doneSpawn` or the chunk lists it as already spawned (`IsoChunk.randomizeBuildingsEtc`).
`doneSpawn` is only set when a room is *spotted*. But `IsoGridSquare.CalcVisibility` and
`checkRoomSeen` mark a room `explored` **before** calling `IsoRoom.onSee`, and `onSee` only spots rooms
that are **not** explored yet. So the room you actually look into is never spotted and never gets
`doneSpawn`; once the chunk's own list doesn't cover it, the room is filled again.

In one Real USA save, 38 of the 78 rooms the player had explored were missing `doneSpawn`.

## The fix

One patch: `IsoChunk.isSpawnedRoom(long)` (the check the game makes right before filling a room)
answers "yes" for any room a player has explored, and sets `doneSpawn` so the save keeps it.
Existing saves are fixed as their areas load. Nothing else changes: unexplored rooms spawn as before.
Log lines start with `[ClearedRooms]`.

## Install

Needs [ZombieBuddy](https://steamcommunity.com/sharedfiles/filedetails/?id=3619862853).
Enable *Cleared Rooms Stay Clear* after ZombieBuddy.

From source: `tools/build.sh` then `tools/install.sh`.
