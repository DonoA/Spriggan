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

/**
 *
 * @author Donovan Allen
 */
public class InstalledPlugin {
    
    private Plugin plugin;
    
    private boolean active;
    
    private File installLocation;
    
    public InstalledPlugin(Plugin plugin, File loc){
        this.plugin = plugin;
        this.installLocation = loc;
        this.active = true;
    }
    
    public File getInstalledLocation(){
        return this.installLocation;
    }
    
    public void setActive(boolean active){
        this.active = active;
    }
    
    public boolean isActive(){
        return this.active;
    }
    
    public Plugin getPlugin(){
        return this.plugin;
    }
}
