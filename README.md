# Cleared Rooms Stay Clear

A Project Zomboid Build 42.22 fix (ZombieBuddy Java mod): houses you cleared, boarded up or lived in
no longer fill up with zombies again.

**[Get it on the Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3840342966)** · or download the zip from [Releases](https://github.com/WilliamJohnDavisIII/pz-cleared-rooms/releases).

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

## Zombies put back on reload (1.1)

The room fill is not the only way zombies end up in a cleared house. When an area unloads or the world
is saved, its zombies go to the native population manager, which keeps simulating them (wandering,
respawn, redistribution) without knowing about the player's walls and barricades. When the area loads,
`ZombiePopulationManager.addZombieStanding` / `addZombieMoving` put them back wherever the population
manager says, checking only for a floor and the player's spawn point. So a sealed base can have zombies
inside after a reload.

1.1 records the persistent ids of the zombies standing in a room when their area unloads
(`removeChunkFromWorld`, `virtualizeZombie`) or the world saves (`MapCollisionData.save`), in
`clearedrooms_inside.bin` in the save. When a zombie is put back on a square in a room a player has
explored, it is created only if its id is on that list; otherwise it is dropped (the same way vanilla
drops zombies near the player's spawn). Zombies that really were inside come back, unexplored rooms and
the outdoors are untouched, and zombies that walk in while the area is loaded are real zombies and are
recorded like any other. A save without the file (older saves, new games) loads every zombie as usual
until it has been saved once.

Tested in 42.22.2 by calling `addZombieStanding` directly: a zombie not on the list was kept out of an
explored kitchen, while zombies for an unexplored room and outdoors were created, and the zombies that
were inside at save time came back after a reload.

## Install

Subscribe on the [Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3840342966), or unzip the release into `Zomboid/mods/`.
Needs [ZombieBuddy](https://steamcommunity.com/sharedfiles/filedetails/?id=3619862853).
Enable *Cleared Rooms Stay Clear* after ZombieBuddy.

From source: `tools/build.sh` then `tools/install.sh`.
