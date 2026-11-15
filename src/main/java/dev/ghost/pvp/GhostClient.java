package dev.ghost.pvp;

import dev.ghost.pvp.config.ConfigManager;
import dev.ghost.pvp.event.EventBus;
import dev.ghost.pvp.friend.FriendManager;
import dev.ghost.pvp.module.ModuleManager;
import dev.ghost.pvp.ui.clickgui.ClickGui;
import dev.ghost.pvp.ui.hud.HudRenderer;
import dev.ghost.pvp.ui.notifications.NotificationManager;
import dev.ghost.pvp.util.Globals;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class GhostClient implements ClientModInitializer {

    public static final String MOD_ID   = "ghostpvp";
    public static final String MOD_NAME = "GhostPvP";
    public static final Logger LOG      = LoggerFactory.getLogger(MOD_NAME);

    private static GhostClient instance;

    private EventBus            eventBus;
    private ModuleManager       moduleManager;
    private ConfigManager       configManager;
    private FriendManager       friendManager;
    private ClickGui            clickGui;
    private HudRenderer         hudRenderer;
    private NotificationManager notificationManager;

    // Debounce flags so single key press doesn't fire every tick
    private boolean guiKeyWasDown   = false;
    private boolean panicKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        instance = this;

        eventBus            = new EventBus();
        configManager       = new ConfigManager();
        friendManager       = new FriendManager();
        moduleManager       = new ModuleManager(eventBus);
        notificationManager = new NotificationManager();
        clickGui            = new ClickGui(moduleManager, configManager);
        hudRenderer         = new HudRenderer(moduleManager, notificationManager);

        configManager.loadAll(moduleManager);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Globals.mc = client;
            eventBus.post(new dev.ghost.pvp.event.events.TickEvent(client));

            if (client.screen == null && client.level != null) {
                long win = client.getWindow().getWindow();

                // RSHIFT → open ClickGUI (edge-triggered)
                boolean guiDown = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (guiDown && !guiKeyWasDown) {
                    client.setScreen(clickGui);
                }
                guiKeyWasDown = guiDown;

                // END → panic: disable all modules (edge-triggered)
                boolean panicDown = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_END) == GLFW.GLFW_PRESS;
                if (panicDown && !panicKeyWasDown) {
                    moduleManager.getModules().stream()
                        .filter(dev.ghost.pvp.module.Module::isEnabled)
                        .forEach(m -> m.setEnabled(false));
                    notificationManager.push("GhostPvP", "Panic — all modules off", 3000);
                }
                panicKeyWasDown = panicDown;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) ->
            hudRenderer.render(drawContext, tickDelta));

        LOG.info("[{}] initialized — {} modules loaded.", MOD_NAME, moduleManager.getModules().size());
    }

    public static GhostClient get()                      { return instance; }
    public EventBus            getEventBus()             { return eventBus; }
    public ModuleManager       getModuleManager()        { return moduleManager; }
    public ConfigManager       getConfigManager()        { return configManager; }
    public FriendManager       getFriendManager()        { return friendManager; }
    public ClickGui            getClickGui()             { return clickGui; }
    public HudRenderer         getHudRenderer()          { return hudRenderer; }
    public NotificationManager getNotificationManager()  { return notificationManager; }
}

