package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.io.*;
import java.text.ParseException;

/** *****************************************************************
 * A class that finds problems in a knowledge base.  It is not meant
 * to be instantiated.
 */
public class Diagnostics {

    private static List LOG_OPS = Arrays.asList("and","or","not","exists",
                                                "forall","=>","<=>","holds");

    /** *****************************************************************
     * Return a list of terms (for a given argument position) that do not 
     * have a specifed relation.
     * @param kb the knowledge base
     * @param rel the relation name
     * @param argnum the argument position of the term
     * @param limit the maximum number of results to return, or -1 if all
     * @param letter the first letter of the term name
     */
    public static ArrayList termsWithoutRelation(KB kb, 
                                                 String rel, 
                                                 int argnum, 
                                                 int limit, 
                                                 char letter) {

        ArrayList result = new ArrayList();
        String term = null;
        ArrayList forms = null;
        Formula formula = null;
        String pred = null;
        Iterator it2 = null;
        boolean isNaN = true;
        synchronized (kb.getTerms()) {
            for (Iterator it = kb.getTerms().iterator(); it.hasNext();) {
                term = (String) it.next();

                // Exclude the logical operators.
                if (LOG_OPS.contains(term)) 
                    continue;

                // Exclude numbers.
                isNaN = true;
                try {
                    double dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (Exception nex) {
                }
                if (isNaN) {
                    forms = kb.ask("arg",argnum,term);
                    if (forms == null || forms.isEmpty()) {
                        if (letter < 'A' || term.charAt(0) == letter) 
                            result.add(term);                
                    }
                    else {
                        boolean found = false;
                        it2 = forms.iterator();
                        while (it2.hasNext()) {
                            formula = (Formula) it2.next();
                            pred = formula.car();
                            if (pred.equals(rel)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            if (letter < 'A' || term.charAt(0) == letter) 
                                result.add(term);                    
                        }
                    }
                }
                if (limit > 0 && result.size() > limit) {
                    result.add("limited to " + limit + " results");
                    break;
                }
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a documentation string.
     */
    public static ArrayList termsWithoutDoc(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithoutDoc(): "); 

        return termsWithoutRelation(kb,"documentation",1,100,' ');                                              
    }

    /** *****************************************************************
     * Return a list of terms that have more than one documentation string.
     */
    public static ArrayList termsWithMultipleDoc(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithMultipleDoc(): "); 

        Set result = new HashSet();
        Set withDoc = new HashSet();
        Formula f = null;
        String term = "";
        String key = "";
        ArrayList forms = kb.ask("arg", 0, "documentation");
        if (!forms.isEmpty()) {
            boolean isNaN = true;
            Iterator it = forms.iterator();
            while (it.hasNext()) {
                f = (Formula) it.next();

                // Append term and language to make a key.
                term = f.getArgument(1);

                isNaN = true;
                try {
                    double dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (Exception nex) {
                }
                if (isNaN) {

                    key = (term + f.getArgument(2));
                    if (withDoc.contains(key)) 
                        result.add(term);                
                    else 
                        withDoc.add(key);
                }
                
                if (result.size() > 99) {
                    result.add("limited to 100 results");
                    break;
                }
            }
        }

        return new ArrayList(result);
    }


    /** *****************************************************************
     * Returns true if term has an explicitly stated parent, or a
     * parent can be inferred from the transitive relation caches,
     * else returns false.
     */
    private static boolean hasParent(KB kb, String term) {
        boolean ans = false;
        try {
            List<String> preds = Arrays.asList("instance", 
                                               "subclass", 
                                               "subAttribute", 
                                               "subrelation", 
                                               "subCollection",
                                               "subentity");
            Set cached = null;
            for (String pred : preds) {
                cached = kb.getCachedRelationValues(pred, term, 1, 2);
                if ((cached != null) && !cached.isEmpty()) {
                    ans = true;
                    break;
                }
            }
            /*
            ArrayList forms = null;
            forms = kb.ask("arg",1,term);
            if (forms != null && !forms.isEmpty()) {
                boolean found = false;
                Iterator it = forms.iterator();
                while (it.hasNext()) {
                    Formula f = (Formula) it.next();
                    pred = f.getArgument(0);
                    if (preds.contains(pred)) {
                        String secondArg = f.getArgument(2);
                        if (secondArg.equals("Entity")) 
                            return true;
                        boolean otherParent = hasParent(kb,secondArg);
                        if (otherParent) 
                            return true;
                    }
                }
            }
            */
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a parent term.
     */
    public static ArrayList termsWithoutParent(KB kb) {

        List excluded = new ArrayList(LOG_OPS);
        excluded.add("Entity");
        System.out.println("INFO in Diagnostics.termsWithoutParent(): "); 
        ArrayList result = new ArrayList();
        boolean isNaN = true;
        int count = 0;
        synchronized (kb.getTerms()) {
            for (Iterator it = kb.getTerms().iterator(); (it.hasNext() && (count < 100));) {
                String term = (String) it.next();
                if (excluded.contains(term)) 
                    continue;
                isNaN = true;
                try {
                    double dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (Exception nex) {
                }
                if (isNaN) {
                    if (!hasParent(kb,term)) {
                        result.add(term); 
                        count++;
                    }
                }
                if (count > 99) 
                    result.add("limited to 100 results");            
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that have parents which are disjoint.
     */
    public static ArrayList childrenOfDisjointParents(KB kb) {

        System.out.println("INFO in Diagnostics.childrenOfDisjointParents(): "); 
        ArrayList result = new ArrayList();
        String term = null;
        String termX = null;
        String termY = null;
        Set parentSet = null;
        Object[] parents = null;
        Set disjoints = null;
        boolean isNaN = true;
        int count = 0;
        boolean contradiction = false;
        synchronized (kb.getTerms()) {
            for (Iterator it = kb.getTerms().iterator(); it.hasNext();) {
                contradiction = false;
                term = (String) it.next();
                isNaN = true;
                try {
                    double dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (Exception nex) {
                }
                if (isNaN) {
                    parentSet = kb.getCachedRelationValues("subclass", term, 1, 2);
                    parents = null;
                    if ((parentSet != null) && !parentSet.isEmpty())
                        parents = parentSet.toArray();            
                    if (parents != null) {
                        for (int i = 0 ; (i < parents.length) && !contradiction ; i++) {
                            termX = (String) parents[i];
                            disjoints = kb.getCachedRelationValues("disjoint", termX, 1, 2);
                            if ((disjoints != null) && !disjoints.isEmpty()) {
                                for (int j = (i + 1) ; j < parents.length ; j++) {
                                    termY = (String) parents[j];
                                    if (disjoints.contains(termY)) {
                                        result.add(term);
                                        contradiction = true;
                                        count++;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (count > 99) {
                    result.add("limited to 100 results");
                    break;
                }
            }
        }
        return result;
    }

    /** *****************************************************************
     * Returns a list of terms, each of which is an instance of some
     * exhaustively decomposed class but is not an instance of any of
     * the subclasses that constitute the exhaustive decomposition.
     * For example, given (instance E A) and (partition A B C D), then
     * E is included in the list of terms to be returned if E is not a
     * instance of B, C, or D.
     */
    public static ArrayList membersNotInAnyPartitionClass(KB kb) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER Diagnostics.membersNotInAnyPartitionClass("
                           + kb.name + ")"); 
        ArrayList result = new ArrayList();
        try {
            Set reduce = new TreeSet();

            // Use all partition statements and all
            // exhaustiveDecomposition statements.
            ArrayList forms = kb.ask("arg",0,"partition");
            if (forms == null) 
                forms = new ArrayList();
            ArrayList forms2 = kb.ask("arg",0,"exhaustiveDecomposition");
            if (forms2 != null) 
                forms.addAll(forms2);
            boolean go = true;
            Iterator it = null;
            Iterator it2 = null;
            Iterator it3 = null;
            for (it = forms.iterator(); go && it.hasNext();) {
                Formula form = (Formula) it.next();
                String parent = form.getArgument(1);
                ArrayList partition = form.argumentsToArrayList(2);
                List instances = kb.getTermsViaPredicateSubsumption("instance", 
                                                                    2, 
                                                                    parent, 
                                                                    1, 
                                                                    true);

                if ((instances != null) && !instances.isEmpty()) {

                    boolean isInstanceSubsumed = false;
                    boolean isNaN = true;
                    String inst = null;
                    for (it2 = instances.iterator(); go && it2.hasNext();) {
                        isInstanceSubsumed = false;
                        isNaN = true;
                        inst = (String) it2.next();

                        // For diagnostics, try to avoid treating
                        // numbers as bonafide terms.
                        try {
                            double dval = Double.parseDouble(inst);
                            isNaN = Double.isNaN(dval);
                        }
                        catch (Exception nex) {
                        }
                        if (isNaN) {
                            for (it3 = partition.iterator(); it3.hasNext();) {
                                String pclass = (String) it3.next();
                                if (kb.isInstanceOf(inst, pclass)) {
                                    isInstanceSubsumed = true;
                                    break;
                                }
                            }

                            if (isInstanceSubsumed) {
                                continue;
                            }
                            else {
                                /*
                                System.out.println("");
                                System.out.println("  >    parent == " + parent);
                                System.out.println("  > partition == " + partition);
                                System.out.println("  >      inst == " + inst);
                                */

                                reduce.add(inst);
                            }
                        }
                        if (reduce.size() > 99) {
                            go = false;
                        }
                    }
                }
            }
            result.addAll(reduce);
            if (result.size() > 99) {
                result.add("limited to 100 results");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT Diagnostics.membersNotInAnyPartitionClass("
                           + kb.name + ")");
        System.out.println("  > result == " + result.size() + " instances");
        // System.out.println("  > "
        //                    + ((System.currentTimeMillis() - t1) / 1000.0)
        //                    + " seconds elapsed time");

        return result;
    }

    /** *****************************************************************
     * Find all terms which do not appear in any implication (rule).
     */
    public static ArrayList termsWithoutRules(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithoutRules(): "); 
        boolean isNaN = true;
        ArrayList result = new ArrayList();
        synchronized (kb.getTerms()) {
            for (Iterator it = kb.getTerms().iterator(); it.hasNext();) {
                String term = (String) it.next();
                isNaN = true;
                try {
                    double dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (Exception nex) {
                }
                if (isNaN) {
                    ArrayList forms = kb.ask("ant",0,term);
                    ArrayList forms2 = kb.ask("cons",0,term);
                    if (((forms == null) || forms.isEmpty()) 
                        && ((forms2 == null) || forms2.isEmpty()))
                        result.add(term);
                }
                if (result.size() > 99) {
                    result.add("limited to 100 results");
                    break;
                }
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return true if a quantifiers in a quantifier list is not found
     * in the body of the statement.
     */
    private static boolean quantifierNotInStatement(Formula f) {

        if (f.theFormula == null || f.theFormula.length() < 1 ||
            !f.listP() || f.empty())
            return false;
        if (!Arrays.asList("forall", "exists").contains(f.car())) {
            Formula f1 = new Formula();
            f1.read(f.car());
            Formula f2 = new Formula();
            f2.read(f.cdr());
            return (quantifierNotInStatement(f1) || quantifierNotInStatement(f2));
        }
        Formula form = new Formula();
        form.read(f.theFormula);
        if (form.car() != null && form.car().length() > 0) {    // This test shouldn't be needed.
            String rest = form.cdr();                   // Quantifier list plus rest of statement
            Formula quant = new Formula();
            quant.read(rest);

            String q = quant.car();                     // Now just the quantifier list.
            String body = quant.cdr();
            quant.read(q);
            ArrayList qList = quant.argumentsToArrayList(0);  // Put all the quantified variables into a list.
            if (rest.indexOf("exists") != -1 || rest.indexOf("forall") != -1) { //nested quantifiers
                Formula restForm = new Formula();
                restForm.read(rest);
                restForm.read(restForm.cdr());
                if (quantifierNotInStatement(restForm)) 
                    return true;
            }
            for (int i = 0; i < qList.size(); i++) {
                String var = (String) qList.get(i);
                if (body.indexOf(var) == -1) 
                    return true;
            }
        }
        return false;
    }

    /** *****************************************************************
     * Find cases where a variable appears in a quantifier list, but not
     * in the body of the quantified expression.  For example
     * (exists (?FOO) (bar ?FLOO Shmoo))
     * @return an ArrayList of Formula(s).
     */
    public static ArrayList quantifierNotInBody(KB kb) {

        System.out.println("INFO in Diagnostics.quantifierNotInBody(): "); 
        ArrayList result = new ArrayList();
        Iterator it = kb.formulaMap.values().iterator();
        Formula form = null;
        while (it.hasNext()) {             // Iterate through all the axioms.
            form = (Formula) it.next();
            if ((form.theFormula.indexOf("forall") != -1) ||
                (form.theFormula.indexOf("exists") != -1)) {
                if (quantifierNotInStatement(form)) {
                    result.add(form);
                    // System.out.println("  " + form);
                }
            }
            if (result.size() > 19) {
                result.add("limited to 20 results");
                return result;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Add a key to a map and a value to the ArrayList corresponding
     * to the key.  Results are a side effect.
     */
    public static void addToMapList(TreeMap m, String key, String value) {

        ArrayList al = (ArrayList) m.get(key);
        if (al == null) {
            al = new ArrayList();
            m.put(key,al);
        }
        if (!al.contains(value)) 
            al.add(value);
    }

    /** *****************************************************************
     * Add a key to a map and a key, value to the map
     * corresponding to the key.  Results are a side effect.
     */
    public static void addToDoubleMapList(TreeMap m, String key1, String key2, String value) {

        TreeMap tm = (TreeMap) m.get(key1);
        if (tm == null) {
            tm = new TreeMap();
            m.put(key1,tm);
        }
        addToMapList(tm,key2,value);
    }

    /** *****************************************************************
     */
    private static void termLinks(KB kb, TreeMap termsUsed, TreeMap termsDefined) {
        List definitionalRelations = Arrays.asList("instance",
                                                   "subclass",
                                                   "domain",
                                                   "documentation",
                                                   "subrelation");
        synchronized (kb.getTerms()) {
            for (Iterator it = kb.getTerms().iterator(); it.hasNext();) { 
                // Check every term in the KB
                String term = (String) it.next();
                ArrayList forms = kb.ask("arg",1,term);     
                // Get every formula with the term as arg 1
                // Only definitional uses are in the arg 1 position
                if (forms != null && forms.size() > 0) {
                    for (int i = 0; i < forms.size(); i++) {
                        Formula formula = (Formula) forms.get(i);
                        String relation = formula.getArgument(0);
                        String filename = formula.sourceFile;
                        if (definitionalRelations.contains(relation)) 
                            addToMapList(termsDefined,term,filename);
                        else
                            addToMapList(termsUsed,term,filename);
                    }
                }

                forms = kb.ask("arg",2,term);   
                ArrayList newform;
                for (int i = 3; i < 7; i++) {
                    newform = kb.ask("arg",i,term);
                    if (newform != null) 
                        forms.addAll(newform);                
                }
                newform = kb.ask("ant",-1,term);
                if (newform != null) 
                    forms.addAll(newform);                
                newform = kb.ask("cons",-1,term);
                if (newform != null) 
                    forms.addAll(newform);                
                newform = kb.ask("stmt",-1,term);
                if (newform != null) 
                    forms.addAll(newform);                
                if (forms != null && forms.size() > 0) {
                    for (int i = 0; i < forms.size(); i++) {
                        Formula formula = (Formula) forms.get(i);
                        String filename = formula.sourceFile;
                        addToMapList(termsUsed,term,filename);
                    }
                }
            }
        }
        return;
    }

    /** *****************************************************************
     */
    private static void fileLinks(KB kb, TreeMap fileDefines, TreeMap fileUses, 
                                  TreeMap termsUsed, TreeMap termsDefined) {

        Iterator it = termsUsed.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList values = (ArrayList) termsUsed.get(key);
            for (int i = 0; i < values.size(); i++) {
                String value = (String) values.get(i);
                addToMapList(fileUses,value,key);
            }
        }
        it = termsDefined.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList values = (ArrayList) termsDefined.get(key);
            for (int i = 0; i < values.size(); i++) {
                String value = (String) values.get(i);
                addToMapList(fileDefines,value,key);
            }
        }
    }

    /** *****************************************************************
     * Return a list of terms that have basic definitional
     * information (instance, subclass, domain, subrelation,
     * documentation) in a KB constituent that also uses terms
     * defined in another file, which would entail a mutual file
     * dependency, rather than a hierarchy of files.
     * @return a TreeMap of file name keys and an ArrayList of the
     *         files on which it depends. The interior TreeMap file
     *         name keys index ArrayLists of terms.  file -depends
     *         on->filenames -that defines-> terms
     */
    private static TreeMap termDependency(KB kb) {

        System.out.println("INFO in Diagnostics.termDependency()");

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is used.
        TreeMap termsUsed = new TreeMap();

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is defined (meaning appearance in an
        // instance, subclass, domain, subrelation, or documentation statement).
        // 
        TreeMap termsDefined = new TreeMap();

        // A map of file names and ArrayList values listing term names defined
        // in the file;
        TreeMap fileDefines = new TreeMap();

        // A map of file names and ArrayList values listing term names used but not defined
        // in the file;
        TreeMap fileUses = new TreeMap();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        TreeMap fileDepends = new TreeMap();

        termLinks(kb,termsUsed,termsDefined);
        fileLinks(kb,fileDefines,fileUses,termsUsed,termsDefined);

        Iterator it = fileUses.keySet().iterator();
        while (it.hasNext()) {
            String fileUsesName = (String) it.next();
            ArrayList termUsedNames = (ArrayList) fileUses.get(fileUsesName);
            for (int i = 0; i < termUsedNames.size(); i++) {
                String term = (String) termUsedNames.get(i);
                ArrayList fileDependencies = (ArrayList) termsDefined.get(term);
                if (fileDependencies != null) {
                    String fileDepend = null;
                    for (int j = 0; j < fileDependencies.size(); j++) {
                        fileDepend = (String) fileDependencies.get(j);
                        if (!fileDepend.equals(fileUsesName)) 
                            addToDoubleMapList(fileDepends,fileUsesName,fileDepend,term);
                    }
                }
            }
        }
        return fileDepends;
    }

    /** *****************************************************************
     * Check the size of the dependency list.
     * @param depend is a map of file name keys and TreeMap values
     *               listing file names on which the given file
     *               depends. The interior TreeMap file name keys
     *               index ArrayLists of terms. file -depends on->
     *               filename -that defines-> terms
     */
    private static int dependencySize(TreeMap depend, String f, String f2) {

        TreeMap tm = (TreeMap) depend.get(f2);
        if (tm != null) {
            ArrayList al = (ArrayList) tm.get(f);
            if (al != null) 
                return al.size();            
        } 
        return -1;
    }

    /** *****************************************************************
     * Show file dependencies.  If two files depend on each other,
     * show only the smaller list of dependencies, under the
     * assumption that that is the erroneous set.
     */
    public static String printTermDependency(KB kb, String kbHref) {

        // A list of String of filename1-filename2 of pairs already examined so that
        // the routine doesn't waste time examining filename2-filename1
        ArrayList examined = new ArrayList();

        StringBuffer result = new StringBuffer();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        TreeMap fileDepends = Diagnostics.termDependency(kb);
        Iterator it = fileDepends.keySet().iterator();
        while (it.hasNext()) {
            String f = (String) it.next();
            // result.append("File " + f + " depends on: ");
            TreeMap tm = (TreeMap) fileDepends.get(f);
            Iterator it2 = tm.keySet().iterator();
            while (it2.hasNext()) {
                String f2 = (String) it2.next();                
                ArrayList al = (ArrayList) tm.get(f2);
                if (al != null && (dependencySize(fileDepends,f,f2) > al.size() || al.size() < 20))
                    //!examined.contains(f + "-" + f2) && !examined.contains(f2 + "-" + f)
                    {  // show mutual dependencies of comparable size
                    result.append("\nFile " + f2 + " dependency size on file " + f + " is " + dependencySize(fileDepends,f,f2) + "<br>\n");
                    result.append("\nFile " + f + " dependency size on file " + f2 + " is " + al.size() + "\n");
                    result.append(" with terms:<br>\n ");
                    for (int i = 0; i < al.size(); i++) {
                        String term = (String) al.get(i);
                        result.append("<a href=\"" + kbHref + "&term=" + term + "\">" + term + "</a>");
                        if (i < al.size()-1) 
                            result.append(", ");
                    }
                    result.append("<P>\n");
                }
                else {
                    int i = dependencySize(fileDepends,f,f2);
                    int j = dependencySize(fileDepends,f2,f);
                    // && !examined.contains(f + "-" + f2) && !examined.contains(f2 + "-" + f)
                    if (i > 0 ) 
                        result.append("\nFile " + f2 + " dependency size on file " + f + " is " + i + "<P>\n");                    
                    if (j > 0 ) 
                        result.append("\nFile " + f + " dependency size on file " + f2 + " is " + j + "<P>\n");                    
                }
                if (!examined.contains(f + "-" + f2)) 
                    examined.add(f + "-" + f2);
            }
            result.append("\n\n");
        }
        return result.toString();
    }

    /** *****************************************************************
     * Make an empty KB for use in Diagnostics. 
     */
    private static KB makeEmptyKB(String kbName) {

        String kbDir = (String)KBmanager.getMgr().getPref("kbDir");
        if (KBmanager.getMgr().existsKB(kbName)) {
            KBmanager.getMgr().removeKB(kbName);
        }
        File dir = new File( kbDir );
        File emptyCFile = new File( dir, "emptyConstituent.txt" );
        String emptyCFilename = emptyCFile.getAbsolutePath();
        FileWriter fw = null; 
        PrintWriter pw = null;
        KBmanager.getMgr().addKB(kbName);
        KB empty = KBmanager.getMgr().getKB(kbName);
        System.out.println("empty = " + empty);

        try { // Fails elsewhere if no constituents, or empty constituent, thus...
            fw = new FileWriter( emptyCFile );
            pw = new PrintWriter(fw);   
            pw.println("(instance instance BinaryPredicate)\n");
            if (pw != null) pw.close();
            if (fw != null) fw.close();
            empty.addConstituent(emptyCFilename);
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + emptyCFilename);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return empty;
    }

    /** *****************************************************************
     * Returns "" if answer is OK, otherwise reports it. 
     */

    private static String reportAnswer(KB kb, String proof, Formula query, String pQuery, String testType) {

        String language = kb.language;
        String kbName = kb.name;
        String hostname = KBmanager.getMgr().getPref("hostname");
        String result = null;
        if (hostname == null || hostname.length() == 0)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null || port.length() == 0)
            port = "8080";
        String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName;
        String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
        StringBuffer html = new StringBuffer();

        if (proof.indexOf("Syntax error detected") != -1) {
            html = html.append("Syntax error in formula : <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>") + "<br><br>");
            result = HTMLformatter.formatProofResult(proof,query.theFormula,
                                                     pQuery,lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }
            
        BasicXMLparser res = new BasicXMLparser(proof);
        ProofProcessor pp = new ProofProcessor(res.elements);
        if (!pp.returnAnswer(0).equalsIgnoreCase("no")) {
            html = html.append(testType + ": <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>") + "<br><br>");
            result = HTMLformatter.formatProofResult(proof,query.theFormula,
                                                     pQuery,lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }
        return "";
    }


    /** *****************************************************************
     * Iterating through all formulas, return a proof of an inconsistent 
     * or redundant one, if such a thing exists.
     */
    public static String kbConsistencyCheck(KB kb) {

        int timeout = 10;
        int maxAnswers = 1;
        String proof;
        String result = null;

        String answer = new String();
        KB empty = makeEmptyKB("consistencyCheck");

        System.out.println("=================== Consistency Testing ===================");
        try {
            Formula theQuery = new Formula();
            Collection allFormulas = kb.formulaMap.values();
            Iterator it = allFormulas.iterator();
            while (it.hasNext()) {
                Formula query = (Formula) it.next();
                ArrayList processedQueries = query.preProcess(false,kb); // may be multiple because of row vars.
                //System.out.println(" query = " + query);
                //System.out.println(" processedQueries = " + processedQueries);

                String processedQuery = null;
                Iterator q = processedQueries.iterator();

                System.out.println("INFO in Diagnostics.kbConsistencyCheck(): size = " + processedQueries.size());
                while (q.hasNext()) {
                    Formula f = (Formula) q.next();
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): formula = " + f.theFormula);
                    processedQuery = f.makeQuantifiersExplicit(false);
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): processedQuery = " + processedQuery);
                    proof = empty.ask(processedQuery,timeout,maxAnswers);
                    String a = reportAnswer(kb,proof,query,processedQuery,"Redundancy");
                    //  if (answer.length() != 0) return answer;
                    answer = answer + a;
                    a = new String();

                    StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");
                    proof = empty.ask(negatedQuery.toString(),timeout,maxAnswers);
                    a = reportAnswer(kb,proof,query,negatedQuery.toString(),"Inconsistency");
                    if (a.length() != 0) {
                        answer = answer + a;
                        return answer;
                    }
                }
                empty.tell(query.theFormula);
            }
        }
        catch ( Exception ex ) {
            return("Error in Diagnostics.kbConsistencyCheck() while executing query: " + ex.getMessage());
        }
        return "No contradictions or redundancies found.";
    }

    /** ***************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        //try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println(termsWithoutParent(kb));

        //}
        //catch (IOException ioe) {
        //    System.out.println("Error in Diagnostics.main(): " + ioe.getMessage());
        //    ioe.printStackTrace();
        //}      
    }
}
