package com.tr1nity.stationsticker.fabric.compat;
public final class RegisteredValue<T> implements java.util.function.Supplier<T>{
 private final java.util.function.Supplier<T> factory;private T value;
 RegisteredValue(java.util.function.Supplier<T> factory){this.factory=factory;}
 public T get(){if(value==null)value=factory.get();return value;}
}