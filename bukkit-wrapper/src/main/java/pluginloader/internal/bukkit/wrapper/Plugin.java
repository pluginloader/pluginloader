package pluginloader.internal.bukkit.wrapper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Plugin extends JavaPlugin implements CommandExecutor {
    static final String versionUrl = "version.txt";
    File dir;
    private String repoUrl;
    String currentVersion;
    File versionFile;
    private Loader currentLoader;

    @Override
    public void onEnable() {
        File config = new File(getFile().getParentFile(), "plugins/wrapper.properties");
        Properties properties = new Properties();
        if(config.exists()){
            try(InputStream in = new BufferedInputStream(new FileInputStream(config))){
                properties.load(in);
            }catch (IOException ex){
                System.out.println("Error on try read config");
                ex.printStackTrace();
            }
        }else{
            properties.setProperty("autoUpdate", "true");
            properties.setProperty("dir", ".plu_cache/plugin-bukkit");
            properties.setProperty("allowRuntimeUpdate", "true");
            properties.setProperty("repoUrl", "https://plu.implario.dev/public/pluginloader-bukkit");
            try{
                File parent = config.getParentFile();
                parent.mkdirs();
                parent.mkdir();
                config.createNewFile();
                try(OutputStream out = new BufferedOutputStream(new FileOutputStream(config))){
                    properties.store(out, "Pluginloader wrapper config");
                }
            }catch (IOException ex){
                System.out.println("Error on try write default config");
                ex.printStackTrace();
            }
        }
        dir = new File(properties.getProperty("dir"));
        if(!dir.exists()){
            dir.mkdirs();
            dir.mkdir();
        }
        versionFile = new File(dir, "version.txt");
        repoUrl = properties.getProperty("repoUrl");
        if(versionFile.exists()){
            try{
                currentVersion = Files.readAllLines(versionFile.toPath()).get(0);
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }else{
            currentVersion = getVersion();
            try{
                versionFile.createNewFile();
                Files.write(versionFile.toPath(), currentVersion.getBytes(StandardCharsets.UTF_8));
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
        File jar = new File(dir, currentVersion + ".jar");
        if(!jar.exists()){
            try {
                jar.createNewFile();
                Files.write(jar.toPath(), get(currentVersion + ".jar"), StandardOpenOption.WRITE);
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
        try {
            currentLoader = new Loader(Plugin.class.getClassLoader(), jar, this, getFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if("true".equals(properties.getProperty("autoUpdate"))){
            Bukkit.getScheduler().runTaskAsynchronously(this, new AsyncCheckVersion(this));
        }
        if("true".equals(properties.getProperty("allowRuntimeUpdate"))) getCommand("plu_update").setExecutor(this);
    }

    @Override
    public void onDisable() {
        if(currentLoader != null) {
            try {
                currentLoader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    byte[] get(String url){
        try {
            URL u = new URI(repoUrl + "/" + url).toURL();
            URLConnection connection = u.openConnection();
            connection.setRequestProperty("User-Agent", "pluginloader");
            connection.connect();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            try (InputStream in = connection.getInputStream()) {
                while (true) {
                    int i = in.read(buffer);
                    if(i <= 0)break;
                    out.write(buffer, 0, i);
                }
            }
            return out.toByteArray();
        }catch (Exception ex){
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    String getVersion(){
        return new String(get(Plugin.versionUrl), StandardCharsets.UTF_8);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.isOp())return true;
        String newVersion = getVersion();
        if(newVersion.equals(currentVersion)) {
            sender.sendMessage("§8[§ai§8]§f Plugin up to date");
            return true;
        }
        File jar = new File(dir, currentVersion + ".jar");
        if(!jar.exists()){
            try {
                jar.createNewFile();
                Files.write(jar.toPath(), get(newVersion + ".jar"));
                Files.write(versionFile.toPath(), newVersion.getBytes(StandardCharsets.UTF_8));
            }catch (Exception ex){
                sender.sendMessage("§8[§ci§8]§f Error :c, check console");
                ex.printStackTrace();
                return true;
            }
        }
        try {
            currentLoader.close();
            currentLoader = new Loader(Plugin.class.getClassLoader(), jar, this, getFile());
            sender.sendMessage("§8[§ai§8]§f Updated");
        }catch (Exception ex){
            sender.sendMessage("§8[§ci§8]§f Error :c, check console");
            ex.printStackTrace();
            return true;
        }
        currentVersion = newVersion;
        return true;
    }
}

class Loader extends ClassLoader implements Closeable{
    private final ZipFile zipFile;
    Class<?> mainClass;
    Object mainObject;

    Loader(ClassLoader parent, File file, JavaPlugin plugin, File f) throws Exception{
        super(parent);
        zipFile = new ZipFile(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(InputStream in = getResourceAsStream("load.txt")) {
            byte[] buffer = new byte[4096];
            while (in.available() > 0) out.write(buffer, 0, in.read(buffer));
        } catch (Exception ex){
            ex.printStackTrace();
        }
        mainClass = loadClass(new String(out.toByteArray(), StandardCharsets.UTF_8).split("\n")[0]);
        mainObject = mainClass.getConstructors()[0].newInstance();
        mainClass.getDeclaredMethod("load", JavaPlugin.class, File.class).invoke(mainObject, plugin, f);
    }

    @Override
    protected Class<?> findClass(String clazz) throws ClassNotFoundException {
        ZipEntry entry = zipFile.getEntry(clazz.replace('.', '/') + ".class");
        if(entry == null)throw new ClassNotFoundException();
        try(InputStream in = zipFile.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (in.available() > 0) out.write(buffer, 0, in.read(buffer));
            return defineClass(clazz, out.toByteArray(), 0, out.size());
        } catch (Exception ex){
            throw new ClassNotFoundException();
        }
    }

    @Nullable
    @Override
    public InputStream getResourceAsStream(String s) {
        ZipEntry entry = zipFile.getEntry(s);
        if(entry == null)return null;
        try {
            return zipFile.getInputStream(entry);
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            mainClass.getDeclaredMethod("unload").invoke(mainObject);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        zipFile.close();
    }
}

class AsyncCheckVersion implements Runnable{
    private final Plugin plugin;

    public AsyncCheckVersion(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String newVersion = plugin.getVersion();
        if(newVersion.equals(plugin.currentVersion))return;
        try{
            File file = new File(plugin.dir, newVersion + ".jar");
            file.createNewFile();
            Files.write(file.toPath(), plugin.get(newVersion + ".jar"));
            Files.write(plugin.versionFile.toPath(), newVersion.getBytes(StandardCharsets.UTF_8));
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
