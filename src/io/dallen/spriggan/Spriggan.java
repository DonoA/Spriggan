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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Scanner;

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
    
    public static void main(String[] argsv) {
        if (argsv.length > 0 && (argsv[0].equalsIgnoreCase("-v") || argsv[0].equalsIgnoreCase("-version"))) {
            System.out.println(VERSION);
            return;
        }
        loadConfig();
        setup();
        Server.loadAll();
        System.out.println("Welcome to Spriggan. Type help for a list of commands.");
        System.out.println("=====");
        Scanner input = new Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.print("> ");
            String[] command = input.nextLine().split(" ");
            try {
                Commands.class.getDeclaredMethod(command[0].toLowerCase(), String[].class).invoke(null, new Object[] {command});
            } catch (NoSuchMethodException ex) {
                System.out.println("Command not found");
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

    }

    private static void loadConfig() {
        File settings = new File("spriggan.conf");
        if (settings.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(settings))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] args = line.split("=");
                    if (args[0].equals("server-folder")) {
                        serverFolder = new File(args[1]);
                    } else if (args[0].equals("maven-folder")) {
                        mavenFolder = new File(args[1]);
                    } else if (args[0].equals("default-version")) {
                        currentSpigot = args[1];
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(settings))) {
                bw.write("#Generated " + new Date().toLocaleString() + "\n");
                serverFolder = new File("servers");
                bw.write("server-folder=" + serverFolder.getPath() + "\n");
                mavenFolder = new File(System.getProperty("user.home") + fsep + ".m2" + fsep + "repository");
                bw.write("maven-folder=" + mavenFolder + "\n");
                bw.write("default-version=" + currentSpigot + "\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }
    
    private static void setup(){
        if(!serverFolder.exists()){
            serverFolder.mkdir();
        }
    }

    public static File getServerFolder() {
        return serverFolder;
    }
    
    public static File getMavenFolder() {
        return mavenFolder;
    }
    
    public static String getDefaultVersion(){
        return currentSpigot;
    }
}
