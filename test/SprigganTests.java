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

import io.dallen.spriggan.InstalledPlugin;
import io.dallen.spriggan.Server;
import io.dallen.spriggan.Spriggan;
import io.dallen.spriggan.TermUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Donovan Allen
 */
public class SprigganTests {
    
    public static boolean success;
    
    private static String srvName = "TestServer";
    
    private static String plgnName = "DallensUtils";
    
    private static String plgnPackage = "io.dallen";
    
    public static void main(String[] argsv) {
        // I really have no idea what im doing here...sorry
        SprigganTests.setUpClass();
        SprigganTests tests = new SprigganTests();
        try {
            tests.testServer();
            tests.testPlugins();
            tests.cleanupServer();
        } catch(Throwable ex) { 
            System.err.println("Tests failed");
            ex.printStackTrace();
            System.out.println("Cleaning up server");
            Server.getServer(srvName).setKeepAlive(false);
            Server.getServer(srvName).stop();
            blockTilStopped(Server.getServer(srvName), 10000);
            Server.getServer(srvName).deleteDataDir();
        }
        System.exit(0);
    }
    
    @BeforeClass
    public static void setUpClass() {
        // Start the app but keep the main loop from running
        Spriggan.running = false;
        PrintStream out = System.out;
        Spriggan.main(new String[] {});
        try {
            System.setOut(new TermUtil(out){
                @Override
                public void println(String m) {
                    this.sysOut.println(m);
                }
            });
        } catch(FileNotFoundException ex){}
    }
    
    @Test
    public void testServer() {
        try {
            // Try to setup server
            Spriggan.handleCommand(new String[] {"create", srvName});
            assertEquals(new File(Spriggan.getServerFolder() + File.separator + srvName).getCanonicalPath(), 
                    Server.getServer(srvName).getDataDir().getCanonicalPath());
            // Connect to it
            Spriggan.handleCommand(new String[] {"connect", srvName});
            assertEquals(Spriggan.getCurrentServer(), Server.getServer(srvName));
            // Check that command can omit the server name
            Spriggan.handleCommand(new String[] {"start"});
            assertTrue(Server.getServer(srvName).isRunning());
            blockTilLoaded(Server.getServer(srvName), 60*1000);
            // disconnect from the serer
            Spriggan.handleCommand(new String[] {"disconnect"});
        } catch (Spriggan.ServerNotFoundException ex) {
            assertTrue(false);
        } catch (Spriggan.CommandNotFoundException ex) {
            assertTrue(false);
        } catch (IOException ex) {
            assertTrue(false);
        }
        // make sure we cant execute 'current'
        success = false;
        try {
            Spriggan.handleCommand(new String[] {"current"});
        } catch (Spriggan.ServerNotFoundException ex){
            success = false;
        } catch (Spriggan.CommandNotFoundException ex){
            success = true;
        } finally {
            assertTrue(success);
        }
        
        success = false;
        try {
            Spriggan.handleCommand(new String[] {"current", srvName});
        } catch (Spriggan.ServerNotFoundException ex){
            success = true;
        } catch (Spriggan.CommandNotFoundException ex){
            success = false;
        } finally {
            assertTrue(success);
        }
        
        Server srv = Server.getServer(srvName);
                
        // make sure shutdown tasks work
        success = false;
        srv.setKeepAlive(true);
        Spriggan.setCurrentServer(srv);
        srv.addShutdownTask(new Runnable() {
            public void run(){
                SprigganTests.success = true;
            }
        });
        srv.stop();
        blockTilLoaded(srv, 60*1000);
        assertTrue(success);
        
        // check conf save and load
        srv.setKeepAlive(false);
        srv.setMemory(1);
        srv.stop();
        blockTilStopped(srv, 30*1000);
        srv.saveConf();
        srv.loadConf();
        assertEquals(srv.getKeepAlive(), false);
        assertEquals(srv.getMemory(), 1);
        
    }
    
    @Test
    public void testPlugins() {
        try {
            Server srv = Server.getServer(srvName);
            if(Spriggan.getCurrentServer() != srv){
                Spriggan.setCurrentServer(srv);
            }
            // Locate a plugin to install
            Spriggan.handleCommand(new String[] {"locate", plgnName});
            assertEquals(new File(Spriggan.getMavenFolder() + File.separator + 
                    plgnPackage.replace('.', File.separatorChar) + File.separator + 
                    plgnName).getAbsolutePath(),
                    ((TermUtil) System.out).getLastOutput());
            // Track plugin
            Spriggan.handleCommand(new String[] {"track", ((TermUtil) System.out).getLastOutput()});
            assertNotNull(Spriggan.getPluginController().getPlugin(plgnName));
            assertTrue(Spriggan.getPluginController().getPlugin(plgnName).getJar().exists());
            Thread.sleep(0);
            // Install the plugin on our server
            Spriggan.handleCommand(new String[] {"install", plgnName});
            InstalledPlugin instPlgn = srv.getPlugins().get(plgnName);
            assertNotNull(instPlgn);
            assertTrue(instPlgn.getInstalledLocation().exists());
            // Start the server
            Spriggan.handleCommand(new String[] {"start"});
            assertTrue(srv.isRunning());
            blockTilLoaded(Server.getServer(srvName), 30*1000);
            // Ensure that updating the plugin restarts the server
            Spriggan.getPluginController().getPlugin(plgnName).getJar().setLastModified(System.currentTimeMillis());
            Thread.sleep(2000);
            blockTilLoaded(Server.getServer(srvName), 30*1000);
            // Ensure that the plugin cannot be removed
            Spriggan.handleCommand(new String[] {"uninstall", plgnName});
            assertNotNull(srv.getPlugins().get(plgnName));
            assertTrue(srv.isRunning());
            // Unit after we restart
            Spriggan.handleCommand(new String[] {"restart"});
            blockTilLoaded(Server.getServer(srvName), 30*1000);
            assertNull(srv.getPlugins().get(plgnName));
            System.out.println("Killing server");
            // Kill the server
            Spriggan.handleCommand(new String[] {"kill"});
            assertEquals(srv.isRunning(), false);
        } catch (Spriggan.ServerNotFoundException ex) {
            assertTrue(false);
        } catch (Spriggan.CommandNotFoundException ex) {
            assertTrue(false);
        } catch (InterruptedException ex) {
        }
    }
    
    @Test
    public void cleanupServer() {
        try {
            Server srv = Server.getServer(srvName);
            if(Spriggan.getCurrentServer() != srv){
                Spriggan.setCurrentServer(srv);
            }
            Spriggan.handleCommand(new String[] {"destroy", srvName});
            assertEquals(srv.getDataDir().exists(), false);
            new File(Spriggan.getPluginFolder() + File.separator + "maven.plugins").delete();
        } catch (Spriggan.ServerNotFoundException ex) {
            assertTrue(false);
        } catch (Spriggan.CommandNotFoundException ex) {
            assertTrue(false);
        }
        System.out.println("All tests passed");
    }
    
    private static void blockTilLoaded(Server srv, int timeout){
        long startTime = System.currentTimeMillis();
        while(!srv.isLoaded() || !srv.isRunning()){
            if(startTime + timeout < System.currentTimeMillis()){
                assertTrue(false);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }
    
    private static void blockTilStopped(Server srv, int timeout){
        long startTime = System.currentTimeMillis();
        while(srv.isLoaded() || srv.isRunning()){
            if(startTime + timeout < System.currentTimeMillis()){
                assertTrue(false);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

}
