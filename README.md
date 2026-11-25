# Carrion (Minecraft 1.20.1 / Forge 47.3.0)

Adds **Carrion**, an amorphous flesh monster (original design, procedural tendril animation). Every other mob hates it: armed mobs hunt it, unarmed ones flee.

| Attack | Behaviour |
|---|---|
| Tendril Lash | two crossing whip sweeps (left arms, then right) |
| Grab | tendril shoots out, hoists the victim, squeezes, then slams or hurls |
| Consume | drags prey into the maw, drains HP, heals and grows on a kill |
| Web | sticky strand that drags the target in |
| Acid Spit | 3+2 blob volley, poison and weakness |
| Lunge | coils, then leaps through the target |
| Spine Burst | sucks enemies in, detonates in a ring of bone spines |
| Roar | shockwave, then Strength/Speed frenzy (at 50% and 20% HP) |
| Mind Control | tendril into a nearby monster, which then fights for Carrion |
| Harden | spiked carapace: 65% less damage, reflects melee hits |
| Hydrophilia | no drowning, fast swimming in water |
| Slash Dash (class 2+) | coil, then a shredding dash that breaks wooden blocks |
| Harpoon Vortex (class 3) | fan of tendril harpoons, then reels the catch back in |
| Echolocation | idle pulse that outlines prey and edible remains |
| Cloak | invisible when far from prey; striking from cloak does +50% damage |

Growth: 4 stages (bigger, tougher, stronger) from eating/killing. Spawn egg in the creative tab; rare natural overworld spawns.

Build: `gradle build` (JDK 21 toolchain, Java 17 bytecode). Jar: `build/libs/carrion-1.0.0.jar`.

## Biomass classes & energy
- Class 1 (0-59 biomass): web, cloak, lash, grab, spit, lunge. Class 2 (60+): + spine burst, slash dash, mind control. Class 3 (160+): + harpoon vortex, carapace. Stage 4 (320+) is the largest.
- Defensive skills (cloak 25, burst 35, carapace 35) cost energy, which recharges over time; the cap grows with class.
- Biomass comes from kills and from reabsorbing meat/rotten flesh drops. Fire resistance grows with class.
- Spawns are mostly class 1, rarely a giant.

## Hive & Cocoon (datapack-configurable)
Any block can be a hive or a cocoon: edit the tags `data/carrion/tags/blocks/hive_blocks.json` and `cocoon_blocks.json` (or override them in a datapack).
- Hive blocks within 3 blocks: heal 4% max HP and +10 energy per second.
- Cocoon blocks within 3 blocks: reabsorbed (+5 biomass) when hurt. A healthy Carrion in water deposits surplus biomass as cocoons.
- Defaults: hive = nether wart block, red mushroom block, shroomlight; cocoon = cobweb, slime block, honey block.
- Items: **Hive Spore** / **Cocoon Spore** (Ingredients tab) convert any right-clicked block (not unbreakable or block entities) into a Hive / Cocoon block, which are in the tags by default.
