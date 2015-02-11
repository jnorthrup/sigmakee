package com.articulate.sigma.semRewrite;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.json.simple.*;
import org.json.simple.parser.*;

import com.articulate.sigma.KBmanager;
import com.google.common.collect.ImmutableList;

import java.io.*;
import java.util.*;

@RunWith(Parameterized.class)
public class InterpreterTest {

    public static Interpreter interp;
 
    @Parameterized.Parameter(value= 0)
    public String fInput;
    @Parameterized.Parameter(value= 1)
    public String fExpected;   
   
    @BeforeClass
    public static void initInterpreter() {
        interp = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interp.loadRules();
    }
    
    @Parameters(name="{0}")
    public static Collection<Object[]> prepare() {
   
   	    	ArrayList<Object[]> result = new ArrayList<Object[]>();
		//String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + 
		String filename = "/home/apease/Sigma/KBs" + File.separator + "gold_standard_translations_notense-small-noex.json";
		JSONParser parser = new JSONParser();  
		try {  
			Object obj = parser.parse(new FileReader(filename));  
			JSONArray jsonObject = (JSONArray) obj; 
			ListIterator<JSONObject> li = jsonObject.listIterator();
			while (li.hasNext()) {
				JSONObject jo = li.next();
				String text = (String) jo.get("text");
				String tokens = (String) jo.get("tokens");
				String type = (String) jo.get("type");
				String kif = (String) jo.get("kif");
				result.add(new Object[]{text,kif});
			}			 
		} 
		catch (FileNotFoundException e) {  
			e.printStackTrace();  
		} 
		catch (IOException e) {  
			e.printStackTrace();  
		} 
		catch (ParseException e) {  
			e.printStackTrace();  
		} 	
		System.out.println(result);
		return result;    
	}
    
    @Test
    public void test() {
        assertEquals(fExpected, interp.interpSingle(fInput));
    }

}
