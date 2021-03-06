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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Donovan Allen
 */
public class PluginController {
    
    private Map<String, Plugin> plugins = new HashMap<String, Plugin>();
    
    private boolean dirty = false;
    
    private static UpdateWatcher updateThread;
    
    public PluginController(){
        for(File plugin : Spriggan.getPluginFolder().listFiles()){
            if(!plugin.isDirectory())
                continue;
            this.addPlugin(plugin.getName(), plugin, false);
        }
        File pluginInfo = new File(Spriggan.getPluginFolder() + File.separator + "maven.plugins");
        if(pluginInfo.exists()){
            Map<String, String> mavenPlugins = ConfUtil.loadConfig(pluginInfo);
            mavenPlugins.forEach((name, location) -> {
                this.addPlugin(name, new File(location), true);
            });
        }
    }
    
    public void setupUpdateThread(){
        updateThread = new UpdateWatcher();
        updateThread.start();
    }
    
    public void addPlugin(String name, File repo, boolean maven){
        dirty = true;
        Plugin p = new Plugin(name, repo, maven);
        plugins.put(name, p);
        updateThread.addPlugin(p);
    }
    
    public void saveIfDirty(){
        if(!dirty)
            return;
        Map<String, String> pln = new HashMap<String, String>();
        plugins.entrySet().stream().filter((e)->{
            return e.getValue().isMaven();
        }).forEach((e)->{
            pln.put(e.getKey(), e.getValue().getLocation().toString());
        });
        ConfUtil.saveConfig(new File(Spriggan.getPluginFolder() + File.separator + "maven.plugins"), pln);
    }
    
    public Plugin getPlugin(String name){
        return plugins.get(name);
    }

    public Map<String, Plugin> getPlugins() {
        return plugins;
    }
    
    public static class UpdateWatcher extends Thread {
        
        public boolean running = true;
        
        private long lastRun = 0;
        
        private volatile Map<String, Long> modified = new HashMap<String, Long>();
        
        public UpdateWatcher(){
            for(Plugin p : Spriggan.getPluginController().getPlugins().values()){
                modified.put(p.getName(), p.getJar().lastModified());
            }
        }
        
        public void addPlugin(Plugin p){
            modified.put(p.getName(), p.getJar().lastModified());
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
                            s.addShutdownTask(() -> {
                                try {
                                    Files.copy(p.getJar().toPath(), new File(s.getDataDir() + File.separator + "plugins" + File.separator + p.getName() + ".jar").toPath(), 
                                            StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                            modified.put(p.getName(), p.getJar().lastModified());
                        }
                    }
                    if(dirty){
                        s.setKeepAlive(true);
                        s.stop();
                    }
                }
                lastRun = System.currentTimeMillis();
            }
        }
    }
    
}
