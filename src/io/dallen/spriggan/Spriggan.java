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
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author Donovan Allen
 */
public class Spriggan {

    private static File serverFolder;

    private static File mavenFolder;
    
    private static File pluginFolder;
    
    private static PluginController pluginController;

    public static final String fsep = System.getProperty("file.separator");

    private static String VERSION = "0.1";

    private static String currentSpigot = "1.11.2";

    private static Server currentServer = null;
    

    public static void main(String[] argsv) {
        if (argsv.length > 0 && (argsv[0].equalsIgnoreCase("-v") || argsv[0].equalsIgnoreCase("-version"))) {
            System.out.print(VERSION);
            return;
        }
        File dataDir = new File("data");
        if(!dataDir.exists()){
            dataDir.mkdir();
        }
        File settings = new File("data" + fsep + "spriggan.conf");
        if (settings.exists()) {
            Map<String, String> data = ConfUtil.loadConfig(settings);
            serverFolder = new File(data.get("server-folder"));
            pluginFolder = new File(data.get("plugin-folder"));
            mavenFolder = new File(data.get("maven-folder"));
            currentSpigot = data.get("default-version");
        } else {
            serverFolder = new File("data" + fsep + "servers");
            pluginFolder = new File("data" + fsep + "plugins");
            mavenFolder = new File(System.getProperty("user.home") + fsep + ".m2" + fsep + "repository");
            ConfUtil.saveConfig(settings, new HashMap<String, String>() {
                {
                    put("server-folder", "data" + fsep + "servers");
                    put("plugin-folder", "data" + fsep + "plugins");
                    put("maven-folder", System.getProperty("user.home") + fsep + ".m2" + fsep + "repository");
                    put("default-version", currentSpigot);
                }
            });
        }
        setupFiles();
        pluginController = new PluginController();
        Server.loadAll();

        try {
            System.setOut(new TermUtil(System.out));
        } catch (FileNotFoundException ex) {
            System.out.println("Failed to bind new sys out");
        }
        System.out.println("Welcome to Spriggan. Type help for a list of commands.");
        System.out.println("=====");
        Scanner input = new Scanner(System.in);
        boolean running = true;
        while (running) {
            String[] command = input.nextLine().split(" ");
            for(int i = 0; i < command.length; i++){
                if(((TermUtil) System.out).getLastOutput() != null){
                    command[i] = command[i].replace("!!", ((TermUtil) System.out).getLastOutput());
                }
            }
            try {
                if (currentServer != null) {
                    try {
                        Commands.class.getDeclaredMethod(command[0].toLowerCase(), new Class[]{Server.class, String[].class})
                                .invoke(null, new Object[]{currentServer, command});
                    } catch (NoSuchMethodException ex) {
                        Commands.class.getDeclaredMethod(command[0].toLowerCase(), new Class[]{String[].class})
                                .invoke(null, new Object[]{command});
                    }
                } else {
                    try {
                        Commands.class.getDeclaredMethod(command[0].toLowerCase(), new Class[]{String[].class})
                                .invoke(null, new Object[]{command});
                    } catch (NoSuchMethodException ex) {
                        if (command.length > 1) {
                            if (!Server.allServers().containsKey(command[1])) {
                                System.out.println("Server not found");
                            } else {
                                Server s = Server.getServer(command[1]);
                                List<String> cmdLst = new ArrayList<String>(Arrays.asList(command));
                                cmdLst.remove(1);
                                command = cmdLst.toArray(command);
                                Method cmd = Commands.class.getDeclaredMethod(command[0].toLowerCase(), new Class[]{Server.class, String[].class});
                                if (!cmd.isAnnotationPresent(Commands.StrictlyCurrentServer.class)) {
                                    cmd.invoke(null, new Object[]{s, command});
                                }
                            }
                        } else {
                            System.out.println("Command not found");
                        }
                    }

                }
            } catch (NoSuchMethodException ex) {
                System.out.println("Command not found");
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

    }

    private static void setupFiles() {
        if (!serverFolder.exists()) {
            serverFolder.mkdir();
        }
        if(!pluginFolder.exists()){
            pluginFolder.mkdir();
        }
    }
    
    public static PluginController getPluginController() {
        return pluginController;
    }
    
    public static File getServerFolder() {
        return serverFolder;
    }
    
    public static File getPluginFolder() {
        return pluginFolder;
    }

    public static Server getCurrentServer() {
        return currentServer;
    }

    public static void setCurrentServer(Server newServer) {
        currentServer = newServer;
    }

    public static File getMavenFolder() {
        return mavenFolder;
    }

    public static String getDefaultVersion() {
        return currentSpigot;
    }
}
