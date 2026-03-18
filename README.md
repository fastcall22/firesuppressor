# Fire Suppressing Cauldrons

Server-side fabric mod that prevents fire from spreading around cauldrons. For use by technical players in servers that have fire tick enabled.

The effect is spherical, centered around the bottommost north-west corner of the cauldron, with a radius controlled by the `fire_spread_radius_around_player` gamerule.


## Technical details

This mod hooks `ServerLevel.canSpreadFireAround` to make an additional check for any PoI-registered cauldrons.  Additionally, this mod hooks `PoiManager.remove` to persist the fire suppressing effect to any cauldrons that are broken.  This is to account for cauldrons moved by flying machines, and lasts for 8 ticks.

While there may be some non-zero performance impact, it should be mitigated by the fact that fire spread should be a relatively rare occurrance.  (Hopefully.)


## TODO

- Fill level to control effect radius. May require registering different PoI types, as to account for the case in which an attempt is made to ignite an air block that is covered by an unloaded cauldron.

- Add other versions of Minecraft to build system. The fire tick game rule and behavior has changed drastically a few times in its history.

- Add Forge variant. Or something.
