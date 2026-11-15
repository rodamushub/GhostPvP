package dev.ghost.pvp.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Simple generic event bus.
 * Listeners are keyed by event class; dispatch is in-order by registration.
 * Thread-safe registration; dispatch happens on the client thread only.
 */
public final class EventBus {

    private final Map<Class<?>, List<ListenerEntry<?>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()))
                 .add(new ListenerEntry<>(listener, 0));
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(Class<T> type, Consumer<T> listener, int priority) {
        List<ListenerEntry<?>> list = listeners.computeIfAbsent(
            type, k -> Collections.synchronizedList(new ArrayList<>()));
        list.add(new ListenerEntry<>(listener, priority));
        list.sort(Comparator.comparingInt(e -> -e.priority));
    }

    public <T extends Event> void unsubscribe(Class<T> type, Consumer<T> listener) {
        List<ListenerEntry<?>> list = listeners.get(type);
        if (list != null) list.removeIf(e -> e.listener == listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> T post(T event) {
        List<ListenerEntry<?>> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (ListenerEntry entry : list) {
            if (event instanceof Cancellable c && c.isCancelled()) break;
            entry.listener.accept(event);
        }
        return event;
    }

    private record ListenerEntry<T>(Consumer<T> listener, int priority) {}
}

