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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Donovan Allen
 */
public class Plugin {

    private String name;

    private File location;
    
    private boolean maven;
    
    public Plugin(String name, File location, boolean maven) {
        this.name = name;
        this.location = location;
        this.maven = maven;
    }
    
    public static File searchRepo(String name) {
        File exec = searchFolder(Spriggan.getMavenFolder(), name);
        return exec;
    }
    
    public static File locateRepo(String name) {
        File exec = new File(Spriggan.getMavenFolder() + fsep + name.replace(".", fsep));
        return exec;
    }
    
    public File getJar(){
        if(maven){
            String lstVersion = this.getLatestMavenVersion();
            return new File(location + fsep + lstVersion + fsep + this.name + "-" + lstVersion + ".jar");
        }else{
            return location;
        }
    }
    
    private String getLatestMavenVersion(){
        long lstMod = 0;
        String vrsn = null;
        for(File version : location.listFiles()){
            if(!version.isDirectory()){
                continue;
            }
            long lm = version.lastModified();
            if(lstMod < lm){
                lstMod = lm;
                vrsn = version.getName();
            }
        }
        return vrsn;
    }

    private static File searchFolder(File folder, String name) {
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                if (f.getName().equalsIgnoreCase(name)) {
                    return f;
                }
                File rtn = searchFolder(f, name);
                if (rtn != null) {
                    return rtn;
                }
            }
        }
        return null;
    }

    public boolean isMaven() {
        return maven;
    }

    public File getLocation() {
        return location;
    }
    
    public String getName(){
        return name;
    }
    
    public static class UpdateWatcher extends Thread {
        
        public boolean running = true;
        
        private long lastRun = 0;
        
        private Map<String, Long> modified = new HashMap<String, Long>();
        
        public UpdateWatcher(){
            for(Plugin p : Spriggan.getPluginController().getPlugins().values()){
                modified.put(p.getName(), p.getJar().lastModified());
            }
        }
        
        public void run(){
            while(running){
                if(lastRun + 100 > System.currentTimeMillis()){
                    Thread.yield();
                    continue;
                }
                List<String> checked = new LinkedList<String>();
                for(Server s: Server.allServers().values()){
                    if(!s.isRunning()){
                        continue;
                    }
                    boolean dirty = false;
                    for(InstalledPlugin pgn : s.getPlugins().values()){
                        Plugin p = pgn.getPlugin();
                        if(checked.contains(p.getName())){
                            continue;
                        }
                        checked.add(p.getName());
                        if(p.getJar().lastModified() > modified.get(p.getName())){
                            dirty = true;
                            s.addRebootTask(() -> {
                                try {
                                    Files.copy(p.getJar().toPath(), new File(s.getDataDir() + fsep + "plugins" + fsep + p.getName() + ".jar").toPath(), 
                                            StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                            modified.put(p.getName(), p.getJar().lastModified());
                        }
                    }
                    if(dirty){
                        s.keepAlive(true);
                        s.stop();
                    }
                }
                lastRun = System.currentTimeMillis();
            }
        }
    }
}
