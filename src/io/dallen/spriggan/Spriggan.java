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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Donovan Allen
 */
public class Spriggan {

    private static File serverFolder;

    private static File mavenFolder;

    public static final String fsep = System.getProperty("file.separator");

    private static String VERSION = "0.1";

    private static String currentSpigot = "1.11.2";

    private static Server currentServer = null;

    public static void main(String[] argsv) {
        if (argsv.length > 0 && (argsv[0].equalsIgnoreCase("-v") || argsv[0].equalsIgnoreCase("-version"))) {
            System.out.println(VERSION);
            return;
        }
        File settings = new File("spriggan.conf");
        if (settings.exists()) {
            Map<String, String> data = ConfUtil.loadConfig(settings);
            serverFolder = new File(data.get("server-folder"));
            mavenFolder = new File(data.get("maven-folder"));
            currentSpigot = data.get("default-version");
        } else {
            serverFolder = new File("servers");
            mavenFolder = new File(System.getProperty("user.home") + fsep + ".m2" + fsep + "repository");
            ConfUtil.saveConfig(settings, new HashMap<String, String>(){{
                put("server-folder", "servers");
                put("maven-folder", System.getProperty("user.home") + fsep + ".m2" + fsep + "repository");
                put("default-version", currentSpigot);
            }});
        }
        setup();
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
//            System.out.print("> ");
            String[] command = input.nextLine().split(" ");
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

    private static void setup() {
        if (!serverFolder.exists()) {
            serverFolder.mkdir();
        }
    }

    public static File getServerFolder() {
        return serverFolder;
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
