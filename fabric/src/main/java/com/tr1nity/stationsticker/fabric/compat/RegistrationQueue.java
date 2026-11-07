package com.tr1nity.stationsticker.fabric.compat;
import net.minecraft.core.Registry;import net.minecraft.resources.ResourceLocation;import java.util.*;import java.util.function.Supplier;
public final class RegistrationQueue<T>{
 private final Registry<T> registry;private final String namespace;private final List<Runnable> entries=new ArrayList<>();
 private RegistrationQueue(Registry<T> registry,String namespace){this.registry=registry;this.namespace=namespace;}
 public static <T> RegistrationQueue<T> create(Registry<T> registry,String namespace){return new RegistrationQueue<>(registry,namespace);}
 public <S extends T> RegisteredValue<S> register(String name,Supplier<S> factory){var ref=new RegisteredValue<>(factory);entries.add(()->Registry.register(registry,new ResourceLocation(namespace,name),ref.get()));return ref;}
 public void register(){entries.forEach(Runnable::run);entries.clear();}
}