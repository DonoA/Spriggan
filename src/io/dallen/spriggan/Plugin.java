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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Donovan Allen
 */
public class Plugin {

    private String name;

    private File repo;

    private List<Plugin> depends = new LinkedList<Plugin>();

    private static Map<String, Plugin> knownPlugins = new HashMap<>();

    public Plugin(String name, File repo) {
        this.name = name;
        this.repo = repo;
    }

    public static File locateRepo(String name) {
        File exec = searchFolder(Spriggan.getMavenFolder(), name);
        return exec;
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
}
