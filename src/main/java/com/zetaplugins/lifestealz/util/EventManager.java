package com.zetaplugins.lifestealz.util;

import com.zetaplugins.lifestealz.annotations.AutoRegisterListener;
import com.zetaplugins.lifestealz.LifeStealZ;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages the registration of event listeners for the LifeStealZ plugin.
 */
public class EventManager {

    private final LifeStealZ plugin;

    public EventManager(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all listener classes annotated with @AutoRegisterListener.
     */
    public void registerAllListeners() {
        Reflections reflections = new Reflections("com.zetaplugins.lifestealz");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(AutoRegisterListener.class);
        List<String> registeredListeners = new ArrayList<>();

        for (Class<?> clazz : annotatedClasses) {
            if (Listener.class.isAssignableFrom(clazz)) {
                String listenerName = registerListener(clazz);
                if (listenerName != null) registeredListeners.add(listenerName);
            }
        }

        registeredListeners.sort(String::compareTo);
        plugin.getLogger().info("Registered Listeners: " + String.join(", ", registeredListeners));
    }

    /**
     * Registers a single listener class.
     * @param listenerClass The listener class to register.
     * @return The name of the registered listener, or null if registration failed.
     */
    private String registerListener(Class<?> listenerClass) {
        try {
            Listener listener;

            try { // Try constructor with plugin parameter first
                Constructor<?> constructor = listenerClass.getConstructor(LifeStealZ.class);
                listener = (Listener) constructor.newInstance(plugin);
            } catch (NoSuchMethodException e) {
                Constructor<?> constructor = listenerClass.getConstructor();
                listener = (Listener) constructor.newInstance();
            }

            AutoRegisterListener annotation = listenerClass.getAnnotation(AutoRegisterListener.class);
            String listenerName = annotation.name().isEmpty() ? listenerClass.getSimpleName() : annotation.name();

            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            return listenerName;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register listener: " + listenerClass.getSimpleName(), e);
            return null;
        }
    }
}
