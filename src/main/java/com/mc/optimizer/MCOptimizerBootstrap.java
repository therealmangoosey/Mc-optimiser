/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.plugin.bootstrap.BootstrapContext
 *  io.papermc.paper.plugin.bootstrap.PluginBootstrap
 *  io.papermc.paper.plugin.bootstrap.PluginProviderContext
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.NotNull
 */
package com.mc.optimizer;

import com.mc.optimizer.OptimizerPlugin;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MCOptimizerBootstrap
implements PluginBootstrap {
    public void bootstrap(@NotNull BootstrapContext context) {
    }

    @NotNull
    public JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new OptimizerPlugin();
    }
}

