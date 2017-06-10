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

import static io.dallen.spriggan.Spriggan.fsep;
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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Donovan Allen
 */
public class Server {

    private final String name;

    private boolean running;

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

    private static final Map<String, Server> servers = new HashMap<>();

    public Server(String name) {
        this(name, Spriggan.getDefaultVersion());
    }

    public Server(String name, String version) {
        this.name = name;
        this.running = false;
        this.dataDir = new File(Spriggan.getServerFolder() + fsep + name);
        this.keepAlive = true;
        this.spigotVersion = version;
        this.logs = new ServerLog(name, (String msg) -> {
            if (Spriggan.getCurrentServer() != null && Spriggan.getCurrentServer().getName().equals(name)) {
                System.out.println(msg);
            }
        });
    }

    public void start() {
        procb = new ProcessBuilder("java", "-DIReallyKnowWhatIAmDoingISwear=\"true\"", "-Xms" + memory + "G", "-Xmx" + memory + "G", "-jar", executable)
                .directory(dataDir)
                .redirectErrorStream(true);
        try {
            System.out.println("Starting " + name);
            running = true;
            proc = procb.start();
            serverMonitor = new ServerHandle();
            serverMonitor.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        try {
            running = false;
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            bw.write("stop\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void addPlugin(Plugin p) {

    }

    public void removePlugin(Plugin p) {

    }

    public void setup() {
        dataDir.mkdir();
        // Find the server jar from maven repo
        File serverJars = new File(Spriggan.getMavenFolder() + fsep + "org" + fsep + "spigotmc" + fsep + "spigot");
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
        File serverJar = new File(serverJars + fsep + exe + fsep + executable);
        try {
            System.out.println("Copying server jar, " + serverJar.getAbsolutePath() + " -> " + new File(dataDir + fsep + executable).getAbsolutePath());
            Files.copy(serverJar.toPath(), new File(dataDir + fsep + executable).toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // Regenerate the proc builder with new exec name
        System.out.println("executing: " + Arrays.toString(new String[] {"java", "-DIReallyKnowWhatIAmDoingISwear=\"true\"", "-Xms" + memory + "G", "-Xmx" + memory + "G", "-jar", executable}));
        procb = new ProcessBuilder("java", "-DIReallyKnowWhatIAmDoingISwear=\"true\"", "-Xms" + memory + "G", "-Xmx" + memory + "G", "-jar", executable)
                .directory(dataDir)
                .redirectErrorStream(true);
        // agree to the eula
        File eula = new File(dataDir + fsep + "eula.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(eula))) {
            bw.write("eula=true");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("Created eula.txt");
        // start the server to create the server.properties
        try {
            System.out.println("Starting server 1st time...");
            Process pr = procb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Preparing start")) {
                    System.out.println("Generating worlds...");
                } else if (line.contains("Done")) {
                    System.out.println("Stopping server 1");
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
                    bw.write("stop\n");
                    bw.flush();
                    bw.close();
                }
            }
            pr.waitFor();
            System.out.println("Server 1 closed");
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        // Edit the server props
        System.out.println("Editing server props");
        File serverProps = new File(dataDir + fsep + "server.properties");
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
            new File(dataDir + fsep + "world" + s).delete();
        }
        // Generate the new flat world
        try {
            System.out.println("Starting server 2nd time...");
            Process pr = procb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Done")) {
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
                    bw.write("stop\n");
                    bw.flush();
                    bw.close();
                }
            }
            pr.waitFor();
            System.out.println("Server 2 closed");
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        System.out.println("Server ready to start and connect on " + host + ":" + port);
        this.saveConf();
    }
    
    public void saveConf(){
        ConfUtil.saveConfig(new File(dataDir + fsep + "spriggan-server.conf"), new HashMap<String, String>(){{
            put("memory", String.valueOf(memory));
            put("keep-alive", String.valueOf(keepAlive));
            put("executable", executable);
            put("spigot-version", spigotVersion);
        }});
    }
    
    public void loadConf(){
        Map<String, String> data = ConfUtil.loadConfig(new File(dataDir + fsep + "spriggan-server.conf"));
        memory = Integer.parseInt(data.get("memory"));
        keepAlive = Boolean.getBoolean(data.get("keep-alive"));
        executable = data.get("executable");
        spigotVersion = data.get("spigot-version");
    }
    
    public void executeCommand(String str){
        if(running){
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
                bw.write(str+"\n");
                bw.flush();
                bw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }else{
            System.out.println("Server not running");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void keepAlive(boolean k) {
        this.keepAlive = k;
    }

    public File getDataDir() {
        return dataDir;
    }

    public void kill() {
        proc.destroy();
    }

    public String getName() {
        return name;
    }

    public static Map allServers() {
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
    
    private class ServerHandle extends Thread {

        private BufferedReader reader;

        private Queue<Runnable> onReboot = new LinkedList<>();

        public ServerHandle() {
            this.reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        }

        public void addRebootTask(Runnable rb) {
            onReboot.add(rb);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.write(line);
                }
                Server.this.proc.waitFor();
                System.out.println(name + " closed");
                reader.close();
                if (Server.this.keepAlive) {
                    while (!onReboot.isEmpty()) {
                        onReboot.poll().run();
                    }
                    Server.this.start();
                } else {
                    running = false;
                }
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}
