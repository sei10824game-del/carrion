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
| Cloak | invisible when far from prey; striking from cloak does +50% damage |

Growth: 4 stages (bigger, tougher, stronger) from eating/killing. Spawn egg in the creative tab; rare natural overworld spawns.

Build: `gradle build` (JDK 21 toolchain, Java 17 bytecode). Jar: `build/libs/carrion-1.0.0.jar`.
