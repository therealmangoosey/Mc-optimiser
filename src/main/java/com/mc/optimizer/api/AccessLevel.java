/*
 * Decompiled with CFR 0.152.
 */
package com.mc.optimizer.api;

enum AccessLevel {
    NONE(0),
    READ_ONLY(1),
    STANDARD(2),
    ADMIN(3);

    private final int level;

    private AccessLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }
}

