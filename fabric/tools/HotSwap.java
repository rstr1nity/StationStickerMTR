import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import javax.tools.ToolProvider;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Watches real project sources, compiles against Fabric mappings and redefines loaded classes. */
public class HotSwap {
 static Path fabric,shared,generated,classes;static Properties config;static boolean old;
 static Map<Path,Long> snapshot() throws Exception {
  Map<Path,Long> result=new TreeMap<>();
  for(Path dir:List.of(shared,fabric.resolve("src/main/java"),fabric.resolve("../src/main/resources").normalize(),fabric.resolve("src/main/resources")))try(var walk=Files.walk(dir)){for(Path p:walk.filter(Files::isRegularFile).toList())result.put(p,Files.getLastModifiedTime(p).toMillis());}
  return result;
 }
 static void sync() throws Exception {
  Set<String> excluded=Set.of("StationStickerMod.java","ClientModEvents.java","ClientCacheEvents.java","ModNetwork.java");
  try(var walk=Files.walk(shared)){for(Path p:walk.filter(p->p.toString().endsWith(".java")).toList()){
   if(excluded.contains(p.getFileName().toString()))continue;
   String text=Files.readString(p).replace("javax.annotation.Nullable","org.jetbrains.annotations.Nullable");
   String[][] replace={{"net.minecraftforge.registries.DeferredRegister","com.tr1nity.stationsticker.fabric.compat.RegistrationQueue"},{"net.minecraftforge.registries.RegistryObject","com.tr1nity.stationsticker.fabric.compat.RegisteredValue"},{"net.minecraftforge.registries.ForgeRegistries","com.tr1nity.stationsticker.fabric.compat.GameRegistries"},{"net.minecraftforge.network.NetworkEvent","com.tr1nity.stationsticker.fabric.compat.PacketContext"},{"net.minecraftforge.network.PacketDistributor","com.tr1nity.stationsticker.fabric.compat.PacketTargets"},{"net.minecraftforge.fml.DistExecutor","com.tr1nity.stationsticker.fabric.compat.EnvironmentExecutor"},{"net.minecraftforge.api.distmarker.Dist","com.tr1nity.stationsticker.fabric.compat.EnvironmentType"},{"DeferredRegister","RegistrationQueue"},{"RegistryObject","RegisteredValue"},{"ForgeRegistries","GameRegistries"},{"NetworkEvent.Context","PacketContext.Context"},{"PacketDistributor","PacketTargets"},{"DistExecutor","EnvironmentExecutor"},{"Dist.CLIENT","EnvironmentType.CLIENT"},{"CreativeModeTab.builder()","net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()"},{"Registries.CREATIVE_MODE_TAB","net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB"}};
   for(var pair:replace)text=text.replace(pair[0],pair[1]);
   text=text.replaceAll("@Override\\s+(public AABB getRenderBoundingBox)","$1");
   if(old && p.getFileName().toString().equals("ModBlocks.java"))text=text.replaceAll("(?s)new net\\.minecraft\\.world\\.item\\.CreativeModeTab\\(\"stationsticker\"\\) \\{.*?\\n    \\};","net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder.build(new net.minecraft.resources.ResourceLocation(\"stationsticker\",\"main\"), () -> new net.minecraft.world.item.ItemStack(ModBlocks.STATION_STICKER.get()));");
   Path dest=generated.resolve(shared.relativize(p));Files.createDirectories(dest.getParent());
   if(!Files.exists(dest)||!Files.readString(dest).equals(text))Files.writeString(dest,text);
  }}
 }
 static void resources() throws Exception {
  var values=new Properties();try(var in=Files.newBufferedReader(fabric.resolve("../gradle.properties"))){values.load(in);}
  values.setProperty("version",values.getProperty("mod_version")+"+"+values.getProperty("minecraft_version"));
  for(Path dir:List.of(fabric.resolve("../src/main/resources").normalize(),fabric.resolve("src/main/resources")))try(var walk=Files.walk(dir)){for(Path p:walk.filter(Files::isRegularFile).toList()){
   String relative=dir.relativize(p).toString().replace('\\','/');if(relative.equals("META-INF/mods.toml"))continue;
   byte[] bytes=Files.readAllBytes(p);
   if(relative.equals("pack.mcmeta")||relative.equals("fabric.mod.json")||relative.startsWith("data/stationsticker/recipes/")){
    String text=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
    for(var entry:values.entrySet())text=text.replace("${"+entry.getKey()+"}",entry.getValue().toString());
    if(relative.startsWith("data/stationsticker/recipes/"))text=text.replace("forge:","stationsticker:");bytes=text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
   }
   Path dest=fabric.resolve("build/resources/main").resolve(relative);Files.createDirectories(dest.getParent());Files.write(dest,bytes);
  }}
 }
 static boolean compile() throws Exception {
  sync();resources();List<String> args=new ArrayList<>(List.of("-encoding","UTF-8","-g","--release","17","-classpath",config.getProperty("classpath"),"-d",classes.toString()));
  for(Path dir:List.of(generated,fabric.resolve("src/main/java")))try(var walk=Files.walk(dir)){walk.filter(p->p.toString().endsWith(".java")).forEach(p->args.add(p.toString()));}
  return ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,args.toArray(String[]::new))==0;
 }
 public static void main(String[] args)throws Exception {
  fabric=Path.of(args[0]).toAbsolutePath();shared=fabric.resolve("../src/main/java").normalize();generated=fabric.resolve("build/generated/shared");classes=fabric.resolve("build/classes/java/main");config=new Properties();try(var in=Files.newInputStream(fabric.resolve("build/hotswap.properties"))){config.load(in);}old=config.getProperty("minecraft").equals("1.19.2");
  int port=Integer.parseInt(config.getProperty("port"));VirtualMachine vm=null;
  var connector=Bootstrap.virtualMachineManager().attachingConnectors().stream().filter(c->c.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();
  System.out.println("Waiting for Fabric debugger on 127.0.0.1:"+port);
  for(int i=0;i<360&&vm==null;i++){var options=connector.defaultArguments();options.get("hostname").setValue("127.0.0.1");options.get("port").setValue(Integer.toString(port));try{vm=connector.attach(options);}catch(java.io.IOException e){Thread.sleep(1000);}}
  if(vm==null)throw new IllegalStateException("Fabric did not open its debugger port");
  final VirtualMachine connected=vm;
  Thread events=new Thread(()->{try{while(true){var event=connected.eventQueue().remove();event.resume();}}catch(InterruptedException|VMDisconnectedException ignored){}},"hotswap-events");events.setDaemon(true);events.start();vm.resume();
  var prepare=vm.eventRequestManager().createClassPrepareRequest();prepare.addClassFilter("com.tr1nity.stationsticker.*");prepare.setSuspendPolicy(com.sun.jdi.request.EventRequest.SUSPEND_NONE);prepare.enable();
  System.out.println("HOT SWAP READY: edit shared src/main/java or fabric/src/main/java; structural changes require restart.");
  if(args.length>1 && args[1].equals("verify")){
   for(int i=0;i<600 && vm.classesByName("com.tr1nity.stationsticker.fabric.FabricClientSmoke").isEmpty();i++)Thread.sleep(500);
   if(!compile())throw new IllegalStateException("Verification compilation failed");
   Map<ReferenceType,byte[]> changes=new HashMap<>();
   for(var type:vm.allClasses())if(type.isPrepared() && type.name().startsWith("com.tr1nity.stationsticker.")){Path p=classes.resolve(type.name().replace('.','/')+".class");if(Files.exists(p))changes.put(type,Files.readAllBytes(p));}
   if(changes.isEmpty())throw new IllegalStateException("No loaded Station Sticker classes");
   vm.redefineClasses(changes);System.out.println("HOT SWAP SELFTEST PASS: redefined "+changes.size()+" actual Fabric classes");vm.dispose();return;
  }
  var previous=snapshot();
  try{while(true){Thread.sleep(700);var next=snapshot();if(next.equals(previous))continue;Thread.sleep(400);previous=snapshot();
   if(!compile()){System.out.println("Compilation failed; running game unchanged. Fix errors and save again.");continue;}
   Map<ReferenceType,byte[]> changes=new HashMap<>();
   for(var type:vm.allClasses())if(type.isPrepared() && type.name().startsWith("com.tr1nity.stationsticker.")){
    Path p=classes.resolve(type.name().replace('.','/')+".class");if(Files.exists(p))changes.put(type,Files.readAllBytes(p));
   }
   try{vm.redefineClasses(changes);System.out.println("HOT SWAP APPLIED: "+changes.size()+" loaded classes");}
   catch(UnsupportedOperationException|VerifyError|ClassFormatError e){System.out.println("RESTART REQUIRED: "+e.getMessage());}
  }}catch(VMDisconnectedException e){System.out.println("Fabric closed; hot swap watcher stopped.");}
 }
}
