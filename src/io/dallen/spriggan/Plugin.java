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
        File exec = new File(Spriggan.getMavenFolder() + File.separator + name.replace(".", File.separator));
        return exec;
    }
    
    public File getJar(){
        if(maven){
            String lstVersion = this.getLatestMavenVersion();
            return new File(location + File.separator + lstVersion + File.separator + this.name + "-" + lstVersion + ".jar");
        }else{
            return location;
        }
    }
    
    public String getLatestMavenVersion(){
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
    
    
}
