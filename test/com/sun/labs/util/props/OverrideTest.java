/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.util.props;

import java.io.IOException;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author stgreen
 */
public class OverrideTest {

    public OverrideTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void overrideWithSameType() throws IOException {
        URL cu = getClass().getResource("overrideConfig.xml");
        ConfigurationManager cm = new ConfigurationManager(cu);
        StringConfigurable sc = (StringConfigurable) cm.lookup("a");
        assertEquals("a", sc.one);
        assertEquals("b", sc.two);
        assertEquals("c", sc.three);
        sc = (StringConfigurable) cm.lookup("b");
        assertEquals("a", sc.one);
        assertEquals("b", sc.two);
        assertEquals("d", sc.three);
    }

    @Test
    public void doubleOverride() throws IOException {
        URL cu = getClass().getResource("overrideConfig.xml");
        ConfigurationManager cm = new ConfigurationManager(cu);
        StringConfigurable sc = (StringConfigurable) cm.lookup("a");
        assertEquals("a", sc.one);
        assertEquals("b", sc.two);
        assertEquals("c", sc.three);
        sc = (StringConfigurable) cm.lookup("b");
        assertEquals("a", sc.one);
        assertEquals("b", sc.two);
        assertEquals("d", sc.three);
        sc = (StringConfigurable) cm.lookup("bsub");
        assertEquals("a", sc.one);
        assertEquals("b", sc.two);
        assertEquals("e", sc.three);
    }
    
    @Test
    public void overrideWithSubType() throws IOException {
        URL cu = getClass().getResource("overrideConfig.xml");
        ConfigurationManager cm = new ConfigurationManager(cu);
        StringConfigurable sc = (StringConfigurable) cm.lookup("a");
        assertEquals("a", sc.one);
        assertEquals("b", sc.two);
        assertEquals("c", sc.three);
        StringleConfigurable sc2 = (StringleConfigurable) cm.lookup("c1");
        assertEquals("a", sc2.one);
        assertEquals("b", sc2.two);
        assertEquals("c", sc2.three);
        assertEquals("e", sc2.four);
        sc2 = (StringleConfigurable) cm.lookup("c2");
        assertEquals("a", sc2.one);
        assertEquals("b", sc2.two);
        assertEquals("d", sc2.three);
        assertEquals("e", sc2.four);
    }
}