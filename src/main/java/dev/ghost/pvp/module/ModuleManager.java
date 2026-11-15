package dev.ghost.pvp.module;

import dev.ghost.pvp.combat.crystal.*;
import dev.ghost.pvp.combat.mace.*;
import dev.ghost.pvp.combat.sword.*;
import dev.ghost.pvp.combat.spear.*;
import dev.ghost.pvp.combat.movement.*;
import dev.ghost.pvp.render.*;
import dev.ghost.pvp.combat.utility.*;
import dev.ghost.pvp.event.EventBus;
import dev.ghost.pvp.event.events.KeyPressEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final List<Module>            modules    = new ArrayList<>();
    private final Map<String, Module>     byName     = new LinkedHashMap<>();
    private final Map<Integer, Module>    byKeybind  = new HashMap<>();

    public ModuleManager(EventBus bus) {
        register(
            // Crystal
            new AutoCrystal(),
            new CrystalOptimizer(),
            new AutoHitCrystal(),
            new AutoDrain(),
            new SafeAnchor(),
            new AutoTotem(),

            // Spear & Attribute
            new AttributeSwap(),
            new AutoLunge(),
            new SpearHelper(),

            // Mace
            new AutoMace(),
            new MaceAssist(),

            // Combat
            new KillAura(),
            new TriggerBot(),
            new AimAssist(),
            new Criticals(),
            new ShieldBreaker(),
            new AutoClicker(),
            new WTap(),
            new Reach(),

            // Movement
            new Velocity(),
            new NoSlow(),
            new Sprint(),
            new NoJumpDelay(),
            new AutoJumpReset(),
            new Speed(),
            new ElytraAssist(),
            new WindChargeMacro(),

            // Render
            new PlayerESP(),
            new CrystalESP(),
            new HoleESP(),
            new StorageESP(),
            new TargetESP(),
            new Tracers(),
            new Fullbright(),
            new FreeCam(),
            new CombatHUD(),
            new NoExplosionParticles(),
            new CustomCrosshair(),

            // Utility
            new FastPlace(),
            new AutoSwitch(),
            new InventoryManager(),
            new AutoArmor(),
            new FakePlayer(),
            new NameProtect(),
            new AntiBot(),
            new Scaffold()
        );

        bus.subscribe(KeyPressEvent.class, this::onKey);
    }

    private void register(Module... mods) {
        for (Module m : mods) {
            modules.add(m);
            byName.put(m.getName().toLowerCase(), m);
        }
    }

    private void onKey(KeyPressEvent e) {
        if (e.action != GLFW.GLFW_PRESS) return;
        Module m = byKeybind.get(e.key);
        if (m != null) m.toggle();
    }

    public void bindKey(Module m, int key) {
        byKeybind.values().remove(m);
        m.setKeybind(key);
        if (key >= 0) byKeybind.put(key, m);
    }

    public Optional<Module>   get(String name)      { return Optional.ofNullable(byName.get(name.toLowerCase())); }
    public List<Module>       getModules()           { return Collections.unmodifiableList(modules); }
    public List<Module>       getByCategory(Category c) {
        return modules.stream().filter(m -> m.getCategory() == c).collect(Collectors.toList());
    }
    public List<Module>       search(String query)   {
        String q = query.toLowerCase();
        return modules.stream()
            .filter(m -> m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))
            .collect(Collectors.toList());
    }
}

