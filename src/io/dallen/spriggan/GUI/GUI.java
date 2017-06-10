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
package io.dallen.spriggan.GUI;

import io.dallen.spriggan.Commands;
import io.dallen.spriggan.GUI.GUI.ConsoleOut;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 *
 * @author Donovan Allen
 */
public class GUI {
    
    private final JFrame appFrame;
    
    public final ConsoleOut out;
    
    private final JTextField in = new JTextField(30);
    
    private final List<String> commandHistory = new ArrayList<String>();
    
    public GUI(String version, int len, int wid){
        appFrame = new JFrame("Spriggan " + version);
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        appFrame.setVisible(true);
        appFrame.setSize(len,wid);
        out = new ConsoleOut(30,40);
        out.setEditable(false);
        JScrollPane scroll = new JScrollPane(out, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        out.setScroll(scroll);
        
        in.setEditable(true);
        in.getActionMap().put("Return", new AbstractAction("Return") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                String[] command = in.getText().split(" ");
                in.setText("");
                new Thread() {
                    public void run(){
                        try {
                            Commands.class.getDeclaredMethod(command[0].toLowerCase(), String[].class).invoke(null, new Object[] {command});
                        } catch (NoSuchMethodException ex) {
                            out.println("Command not found");
                        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            ex.printStackTrace();
                        }
                    }
                }.run();
            }
        });
        
        in.getActionMap().put("Up", new AbstractAction("Up") {
            @Override
            public void actionPerformed(ActionEvent ev) {
            }
        });
        
        in.getActionMap().put("Down", new AbstractAction("Down") {
            @Override
            public void actionPerformed(ActionEvent ev) {
            }
        });
        
        in.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "Return");
        in.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "Up");
        in.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "Down");
        
        appFrame.getContentPane().add(scroll, BorderLayout.NORTH);
        appFrame.getContentPane().add(in, BorderLayout.SOUTH);
        appFrame.pack();
    }
    
    public static class ConsoleOut extends JTextArea {
        
        private JScrollPane scroll;
        
        public ConsoleOut(int len, int wid) {
            super(len, wid);
        }
        
        public void println(String msg){
            System.out.println(msg);
//            super.append(msg + "\n");
            checkScroll();
        }
        
        public void print(String msg){
            System.out.print(msg);
//            super.append(msg);
            checkScroll();
        }
        
        public void setScroll(JScrollPane scroll){
            this.scroll = scroll;
        }
        
        private void checkScroll(){
            scroll.validate();
            scroll.getVerticalScrollBar().validate();
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        }
        
    }
    
}
