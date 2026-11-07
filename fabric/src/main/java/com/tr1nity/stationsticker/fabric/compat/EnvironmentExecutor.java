package com.tr1nity.stationsticker.fabric.compat;
public final class EnvironmentExecutor {
 public static void unsafeRunWhenOn(EnvironmentType environment,java.util.function.Supplier<Runnable> action){if(net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()==net.fabricmc.api.EnvType.CLIENT)action.get().run();}
}