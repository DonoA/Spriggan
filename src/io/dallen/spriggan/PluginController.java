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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Donovan Allen
 */
public class PluginController {
    
    private Map<String, Plugin> plugins = new HashMap<String, Plugin>();
    
    private boolean dirty = false;
    
    public PluginController(){
        for(File plugin : Spriggan.getPluginFolder().listFiles()){
            if(!plugin.isDirectory())
                continue;
            this.addPlugin(plugin.getName(), plugin, false);
        }
        File pluginInfo = new File(Spriggan.getPluginFolder() + fsep + "maven.plugins");
        if(pluginInfo.exists()){
            Map<String, String> mavenPlugins = ConfUtil.loadConfig(pluginInfo);
            mavenPlugins.forEach((name, location) -> {
                this.addPlugin(name, new File(location), true);
            });
        }
    }
    
    public void addPlugin(String name, File repo, boolean maven){
        dirty = true;
        plugins.put(name, new Plugin(name, repo, maven));
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
        ConfUtil.saveConfig(new File(Spriggan.getPluginFolder() + fsep + "maven.plugins"), pln);
    }
    
    public Plugin getPlugin(String name){
        return plugins.get(name);
    }

    public Map<String, Plugin> getPlugins() {
        return plugins;
    }
    
}
