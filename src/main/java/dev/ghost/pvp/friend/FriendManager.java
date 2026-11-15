package dev.ghost.pvp.friend;

import java.util.*;

public final class FriendManager {

    private final Set<String> friends = new LinkedHashSet<>();

    public void add(String name)    { friends.add(name.toLowerCase()); }
    public void remove(String name) { friends.remove(name.toLowerCase()); }
    public boolean isFriend(String name) { return friends.contains(name.toLowerCase()); }
    public Set<String> getAll()     { return Collections.unmodifiableSet(friends); }
    public void clear()             { friends.clear(); }
}

