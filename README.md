# GhostPvP — Fabric PvP Client Mod for Minecraft 26.3

## Build Requirements
- Java 25 JDK
- Gradle 8.10 (or use `./gradlew`)
- Internet connection (Gradle downloads MC + Fabric deps on first build)

## Build
```bash
./gradlew build
```
Output: `build/libs/ghostpvp-1.0.0.jar`

Copy the jar to `.minecraft/mods/` alongside Fabric Loader 0.20.1 and Fabric API 0.165.0+26.3.

## Keybinds
| Key         | Action                              |
|-------------|-------------------------------------|
| RIGHT SHIFT | Open ClickGUI                       |
| END         | Panic — instantly disable all mods  |
| Middle-click (in GUI) | Set module keybind       |
| Right-click (in GUI)  | Expand module settings   |

## ClickGUI
- **Drag** panels by their category header
- **Search** bar at top center filters modules live
- **Theme** button cycles Dark / Rose / Ocean
- **Profile** button cycles saved config profiles
- **Save** button writes current state to active profile
- Configs saved to: `.minecraft/ghostpvp/profiles/<name>.json`

## Module Categories
| Category   | Modules |
|------------|---------|
| Crystal PvP | AutoCrystal, CrystalOptimizer, AutoHitCrystal, AutoDrain, SafeAnchor, AutoTotem |
| Spear & Attr | AttributeSwap, AutoLunge, SpearHelper |
| Mace PvP | AutoMace, MaceAssist |
| Combat | KillAura, TriggerBot, AimAssist, Criticals, ShieldBreaker, AutoClicker, W-Tap, Reach |
| Movement | Velocity, NoSlow, Sprint, NoJumpDelay, AutoJumpReset, Speed, ElytraAssist, WindChargeMacro |
| Render | PlayerESP, CrystalESP, HoleESP, StorageESP, TargetESP, Tracers, Fullbright, FreeCam, CombatHUD, NoExplosionParticles, CustomCrosshair |
| Utility | FastPlace, AutoSwitch, InventoryManager, AutoArmor, FakePlayer, NameProtect, AntiBot, Scaffold |

## Notes
- MC 26.x ships deobfuscated (Mojang mappings used directly, no Yarn)
- Some mixin field names (jump delay, motion packet fields) are marked
  "per-build derivation required" — verify against your specific 26.3 jar
  with a decompiler if a mixin fails to inject
- FakePlayer dummy is client-only and not visible to other players
