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
public class Commands {

    public static void start(String[] args) {
        if (!Server.allServers().containsKey(args[1])) {
            System.out.println("Server not found");
            return;
        }
        Server s = Server.getServer(args[1]);
        if (!s.isRunning()) {
            s.start();
        } else {
            System.out.println("Server already running");
        }
    }

    public static void stop(String[] args) {
        if (!Server.allServers().containsKey(args[1])) {
            System.out.println("Server not found");
            return;
        }
        Server s = Server.getServer(args[1]);
        if (s.isRunning()) {
            s.keepAlive(false);
            s.stop();
        } else {
            System.out.println("Server not running");
        }
    }
    
    public static void kill(String[] args) {
        if (!Server.allServers().containsKey(args[1])) {
            System.out.println("Server not found");
            return;
        }
        Server s = Server.getServer(args[1]);
        if (s.isRunning()) {
            s.kill();
        } else {
            System.out.println("Server not running");
        }
    }
    
    public static void restart(String[] args) {
        if (!Server.allServers().containsKey(args[1])) {
            System.out.println("Server not found");
            return;
        }
        Server s = Server.getServer(args[1]);
        if (s.isRunning()) {
            s.keepAlive(true);
            s.stop();
        } else {
            System.out.println("Server not running");
        }
    }

    public static void help(String[] args) {
        System.out.println("Help!");
    }

    public static void create(String[] args) {
        if (Server.allServers().containsKey(args[1])) {
            System.out.println("Server already exists");
        } else {
            Server s = new Server(args[1]);
            s.setup();
            Server.add(s);
        }
    }

    public static void destroy(String[] args) {
        if (!Server.allServers().containsKey(args[1])) {
            System.out.println("Server not found");
        } else {
            Server s = Server.getServer(args[1]);
            if(s.isRunning())
                s.stop();
            s.getDataDir().delete();
        }
    }
}
