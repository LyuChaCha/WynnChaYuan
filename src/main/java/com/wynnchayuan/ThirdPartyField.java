package com.wynnchayuan;

import java.lang.reflect.Field;

// @Shadow 找不到欄位時遊戲開不起來；用反射的話對方改名只會失去這一項
public final class ThirdPartyField {

    private record Resolved(Class<?> type, Field field) {}

    private final String name;
    private volatile Resolved resolved;

    public ThirdPartyField(String name) {
        this.name = name;
    }

    public Object get(Object owner) {
        Field found = resolve(owner);
        if (found == null) {
            return null;
        }
        try {
            return found.get(owner);
        } catch (IllegalAccessException | RuntimeException failure) {
            ModLog.error("ThirdPartyField#" + name, failure);
            return null;
        }
    }

    public boolean set(Object owner, Object value) {
        Field found = resolve(owner);
        if (found == null) {
            return false;
        }
        try {
            found.set(owner, value);
            return true;
        } catch (IllegalAccessException | RuntimeException failure) {
            ModLog.error("ThirdPartyField#" + name, failure);
            return false;
        }
    }

    public int getInt(Object owner, int fallback) {
        Field found = resolve(owner);
        if (found == null) {
            return fallback;
        }
        try {
            return found.getInt(owner);
        } catch (IllegalAccessException | RuntimeException failure) {
            ModLog.error("ThirdPartyField#" + name, failure);
            return fallback;
        }
    }

    public boolean available(Object owner) {
        return resolve(owner) != null;
    }

    private Field resolve(Object owner) {
        if (owner == null) {
            return null;
        }
        Class<?> type = owner.getClass();
        Resolved known = resolved;
        if (known != null && known.type() == type) {
            return known.field();
        }
        Field found = find(type);
        resolved = new Resolved(type, found);
        return found;
    }

    private Field find(Class<?> type) {
        for (Class<?> at = type; at != null; at = at.getSuperclass()) {
            try {
                Field candidate = at.getDeclaredField(name);
                candidate.setAccessible(true);
                return candidate;
            } catch (NoSuchFieldException absent) {
                continue;
            } catch (RuntimeException failure) {
                ModLog.error("ThirdPartyField#" + name, failure);
                return null;
            }
        }
        ModLog.warn("第三方模組的欄位 " + name + " 不見了（" + type.getName() + "），這一項停用");
        return null;
    }
}
