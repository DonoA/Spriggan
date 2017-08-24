/*
 * Copyright 2017 Donovan Allen.
 * 
 * This file is part of Spriggan.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 */
package io.dallen.spriggan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Donovan Allen
 */
public class Server {

    private final String name;

    private AtomicBoolean running = new AtomicBoolean();
    
    private AtomicBoolean loaded = new AtomicBoolean();

    private File dataDir;

    private ProcessBuilder procb;

    private Process proc;

    private ServerHandle serverMonitor;

    private boolean keepAlive;

    private String executable = "";

    private String spigotVersion;

    private String host = "localhost";

    private int port = 25565;

    private int memory = 2;

    private ServerLog logs;
    
    private Map<String, InstalledPlugin> plugins = new HashMap<String, InstalledPlugin>();
    
    private File serverJar;

    private static final Map<String, Server> servers = new HashMap<>();

    public Server(String name) {
        this(name, Spriggan.getDefaultVersion());
    }

    public Server(String name, String version) {
        this.name = name;
        this.running.set(false);
        this.loaded.set(false);
        this.dataDir = new File(Spriggan.getServerFolder() + File.separator + name);
        this.keepAlive = true;
        this.spigotVersion = version;
        this.logs = new ServerLog(name, (String msg) -> {
            if (Spriggan.getCurrentServer() != null && Spriggan.getCurrentServer().getName().equals(name)) {
                System.out.println(msg);
            }
        });
        
    }

    public void start() {
//        procb = new ProcessBuilder("java", "-DIReallyKnowWhatIAmDoingISwear=\"true\"", "-Xms" + memory + "G", "-Xmx" + memory + "G", "-jar", executable)
//                .directory(dataDir)
//                .redirectErrorStream(true);
        try {
            System.out.println("Starting " + name);
            running.set(true);
            proc = procb.start();
            serverMonitor = new ServerHandle();
            serverMonitor.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        try {
            loaded.set(false);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            bw.write("stop\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean addPlugin(Plugin p) {
        if(running.get()){
            this.addShutdownTask(()->{
                this.addPlugin(p);
                System.out.println("Installed " + p.getName() + " version " + p.getLatestMavenVersion());
            });
            return false;
        }
        try {
            Files.copy(p.getJar().toPath(), new File(dataDir + File.separator + "plugins" + File.separator + p.getName() + ".jar").toPath(), 
                    StandardCopyOption.REPLACE_EXISTING);
            plugins.put(p.getName(), new InstalledPlugin(p, new File(dataDir + File.separator + "plugins" + File.separator + p.getName() + ".jar")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public boolean removePlugin(Plugin p) {
        if(running.get()){
            this.addShutdownTask(()->{
                this.removePlugin(p);
            });
            return false;
        }
        plugins.remove(p.getName()).getInstalledLocation().delete();
        return true;
    }

    public void setup() {
        dataDir.mkdir();
        // Find the server jar from maven repo
        this.installNewJar(spigotVersion);
        // Regenerate the proc builder with new exec name
        System.out.println("executing: " + Arrays.toString(new String[]{"java", "-DIReallyKnowWhatIAmDoingISwear=\"true\"", "-Xms" + memory + "G", "-Xmx" + memory + "G", "-jar", executable}));
        procb = new ProcessBuilder("java", "-DIReallyKnowWhatIAmDoingISwear=\"true\"", "-Xms" + memory + "G", "-Xmx" + memory + "G", "-jar", executable)
                .directory(dataDir)
                .redirectErrorStream(true);
        // agree to the eula
        File eula = new File(dataDir + File.separator + "eula.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(eula))) {
            bw.write("eula=true");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("Created eula.txt");
        // start the server to create the server.properties
        System.out.println("Starting server 1st time...");
        this.start();
        this.blockTilLoaded();
        this.stop();
        this.blockTilStopped();
        System.out.println("Server 1 closed");
        // Edit the server props
        System.out.println("Editing server props");
        File serverProps = new File(dataDir + File.separator + "server.properties");
        try {
            List<String> lines = new LinkedList<String>();
            BufferedReader br = new BufferedReader(new FileReader(serverProps));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(serverProps));
            for (String ln : lines) {
                if (ln.startsWith("level-type=")) {
                    ln = "level-type=FLAT";
                } else if (ln.startsWith("server-ip=")) {
                    ln = "server-ip=" + host;
                } else if (ln.startsWith("server-port=")) {
                    ln = "server-port=" + port;
                } else if (ln.startsWith("difficulty=")) {
                    ln = "difficulty=0";
                } else if (ln.startsWith("gamemode=")) {
                    ln = "gamemode=1";
                }
                bw.write(ln + System.lineSeparator());
            }
            bw.flush();
            bw.close();
        } catch (Exception e) {
            System.out.println("Problem reading file.");
        }
        System.out.println("Regenerating worlds to be flat");
        // delete old world
        for (String s : new String[]{"", "_nether", "_the_end"}) {
            new File(dataDir + File.separator + "world" + s).delete();
        }
        // Generate the new flat world
        System.out.println("Starting server 2nd time...");
        this.start();
        this.blockTilLoaded();
        this.stop();
        this.blockTilStopped();
        System.out.println("Server 2 closed");
        System.out.println("Server ready to start and connect on " + host + ":" + port);
        this.saveConf();
    }

    public void saveConf() {
        ConfUtil.saveConfig(new File(dataDir + File.separator + "spriggan-server.conf"), new HashMap<String, String>() {
            {
                put("memory", String.valueOf(memory));
                put("keep-alive", String.valueOf(keepAlive));
                put("executable", executable);
                put("spigot-version", spigotVersion);
            }
        });
    }

    public void loadConf() {
        Map<String, String> data = ConfUtil.loadConfig(new File(dataDir + File.separator + "spriggan-server.conf"));
        memory = Integer.parseInt(data.get("memory"));
        keepAlive = Boolean.getBoolean(data.get("keep-alive"));
        executable = data.get("executable");
        spigotVersion = data.get("spigot-version");
        for(File f : new File(dataDir + File.separator + "plugins").listFiles()){
            if(!f.getName().endsWith(".jar")){
                continue;
            }
            String name = f.getName().replace(".jar", "");
            plugins.put(name, new InstalledPlugin(Spriggan.getPluginController().getPlugin(name), f));

        }
    }

    public void executeCommand(String str) {
        if (running.get() && loaded.get()) {
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
                bw.write(str + "\n");
                bw.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Server not running");
        }
    }

    public void deleteDataDir(){
        deleteDir(dataDir);
        dataDir.delete();
    }
    
    private void deleteDir(File dir){
        for(File f : dir.listFiles()){
            if(f.isDirectory()){
                deleteDir(f);
            }
            f.delete();
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public boolean isLoaded() {
        return loaded.get();
    }

    public void setKeepAlive(boolean k) {
        this.keepAlive = k;
    }
    
    public boolean getKeepAlive(){
        return this.keepAlive;
    }

    public File getDataDir() {
        return dataDir;
    }
    
    public void setMemory(int memory){
        this.memory = memory;
    }
    
    public int getMemory(){
        return this.memory;
    }

    public void kill() {
        this.running.set(false);
        proc.destroy();
        this.loaded.set(false);
    }

    public String getName() {
        return name;
    }

    public Map<String, InstalledPlugin> getPlugins(){
        return this.plugins;
    }
    
    public static Map<String, Server> allServers() {
        return servers;
    }

    public static Server getServer(String name) {
        return servers.get(name);
    }
    
    public static void loadAll() {
        for (File s : Spriggan.getServerFolder().listFiles()) {
            if (s.isFile() || s.isHidden()) {
                continue;
            }
            Server ns = new Server(s.getName());
            ns.loadConf();
            servers.put(s.getName(), ns);
        }
    }

    public static void add(Server s) {
        if (!servers.containsKey(s.getName())) {
            servers.put(s.getName(), s);
        }
    }
    
    public void addShutdownTask(Runnable rb) {
        serverMonitor.addShutdownTask(rb);
    }
    
    public synchronized void blockTilLoaded(){
        while(!loaded.get() || !running.get()){
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }
    }
    
    public synchronized void blockTilStopped(){
        while(loaded.get() || running.get()){
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }
    }
    
    public File getServerJar() {
        return serverJar;
    }
    
    public boolean installNewJar(String version) {
        if(running.get()){
            this.addShutdownTask(()->{
                this.installNewJar(version);
                System.out.println("New jar installed: " + this.getExecutable());
            });
            return false;
        }
        // Find the server jar from maven repo
        File serverJars = new File(Spriggan.getMavenFolder() + File.separator + "org" + File.separator + "spigotmc" + File.separator + "spigot");
        int R = 0;
        String exe = null;
        for (File f : serverJars.listFiles()) {
            if (!f.getName().startsWith(spigotVersion) || f.isFile()) {
                continue;
            }
            int r = Integer.parseInt(String.valueOf(f.getName().split("-")[1].charAt(3)));
            if (r > R) {
                R = r;
                exe = f.getName();
            }
        }
        executable = "spigot-" + exe + ".jar";
        serverJar = new File(serverJars + File.separator + exe + File.separator + executable);
        try {
            System.out.println("Copying server jar, " + serverJar.getAbsolutePath() + " -> " + new File(dataDir + File.separator + executable).getAbsolutePath());
            Files.copy(serverJar.toPath(), new File(dataDir + File.separator + executable).toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return true;
    }
    
    public String getExecutable(){
        return executable;
    }

    private class ServerHandle extends Thread {

        private BufferedReader reader;

        private Queue<Runnable> onShutdown = new LinkedList<Runnable>();

        public ServerHandle() {
            this.reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        }

        public void addShutdownTask(Runnable rb) {
            onShutdown.add(rb);
        }

        @Override
        public synchronized void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.write(line);
                    if (line.contains("Done")) {
                        Server.this.loaded.set(true);
                        notifyAll();
                    }
                    Thread.yield();
                }
                Server.this.proc.waitFor();
                System.out.println(name + " closed");
                new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())).close();
                reader.close();
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            } finally {
                running.set(false);
                notifyAll();
                while (!onShutdown.isEmpty()) {
                    onShutdown.poll().run();
                }
                if (Server.this.keepAlive) {
                    Server.this.start();
                }

            }
        }
    }

}
