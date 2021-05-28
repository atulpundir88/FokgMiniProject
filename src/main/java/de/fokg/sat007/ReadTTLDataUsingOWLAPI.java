package de.fokg.sat007;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

/**
 * @author Atul
 *
 */
public class ReadTTLDataUsingOWLAPI {

	public static void main(String[] args) throws OWLOntologyCreationException {
		
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLOntology o = m.loadOntologyFromOntologyDocument(new File("kg-mini-project-train.ttl")); // Load file
		OWLDataFactory df = OWLManager.getOWLDataFactory();
		
		// Get data in Hash Map
		HashMap<String, HashMap<String, ArrayList<String>>> mapOfData =	readData(o, df);
		System.out.println("Size of Map : " + mapOfData.size());

		//Create files as per data in HashMap
		deleteOldFiles(mapOfData);//Delete old files if any
		createNewFiles(mapOfData);
		
	}
	
	/**
	 * @param mapOfData
	 */
	private static void createNewFiles(HashMap<String, HashMap<String, ArrayList<String>>> mapOfData) {
		Set<String> lpKeys = mapOfData.keySet();//Get Learning Problem Keys
		for(String lpKey: lpKeys ) {
			String fileName = lpKey.replaceAll("https://lpbenchgen.org/resource/", "");
			
			//Create positive and negative files for each learning problem
			File file = new File("lpfiles/" + fileName + "_p.txt");
			File file1 = new File("lpfiles/" + fileName + "_n.txt");
			
			try {
				file.createNewFile();
				file1.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Issue while creating a file");
			}
			
			try {
				//Write data for positive samples
				FileOutputStream fileOut = new FileOutputStream(file);
				ArrayList<String> listIncludeResources = mapOfData.get(lpKey).get("<https://lpbenchgen.org/property/includesResource>");
				for(String pos: listIncludeResources) {
					fileOut.write(pos.getBytes());
				}
				fileOut.close();

				FileOutputStream fileOut1 = new FileOutputStream(file1);
				ArrayList<String> mapExcludeResources = mapOfData.get(lpKey).get("<https://lpbenchgen.org/property/excludesResource>");
				for(String neg: mapExcludeResources) {
					fileOut1.write(neg.getBytes());
				}
				fileOut1.close();
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("Issue in finding the new file!");
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Issue when writing in the new file!");
			}

		}
	}
	
	/**
	 * @param mapOfData
	 */
	private static void deleteOldFiles(HashMap<String, HashMap<String, ArrayList<String>>> mapOfData) {
		Set<String> lpKeys = mapOfData.keySet();//Get Learning Problem Keys
		for(String lpKey: lpKeys ) {
			String fileName = lpKey.replaceAll("https://lpbenchgen.org/resource/", "");
			File file = new File("lpfiles/" + fileName + "_p.txt");
			File file1 = new File("lpfiles/" + fileName + "_n.txt");
			file.delete();
			file1.delete();
		}
	}

	/**
	 * @param o
	 * @param df
	 * @return
	 */
	private static HashMap<String, HashMap<String, ArrayList<String>>> readData(OWLOntology o, OWLDataFactory df) {
		
		// Initialize Map
		HashMap<String, HashMap<String, ArrayList<String>>> mapOfData = new HashMap<>();
		
		// Code for class
		/*OWLClass clas = null;
		Set<OWLClass> classSet = o.classesInSignature().collect(Collectors.toSet());
		for (OWLClass cls : classSet) {
			//System.out.println(cls);
			clas = cls;
		}*/

		//Walker to get the values from file.
		OWLOntologyWalker walker = new OWLOntologyWalker(Collections.singleton(o));
		OWLOntologyWalkerVisitor visitor = new OWLOntologyWalkerVisitor(walker) {

			public void visit(OWLAnnotationAssertionAxiom axiom) {
				OWLAnnotationValue value = axiom.getValue(); // chemical element
				OWLAnnotationSubject subject = axiom.getSubject();// learning problem id
				OWLAnnotation annotation = axiom.getAnnotation();// property either include or exclude 
				//Set<OWLAnnotationProperty> annotationPropertiesInSignature = axiom.annotationPropertiesInSignature().collect(Collectors.toSet());
				
				// Logic to store data in map
				if (mapOfData.get(subject.toString()) == null) { // If learning problem not present in the map then create a entry
					HashMap<String, ArrayList<String>> resMap = new HashMap<>();
					ArrayList<String> tempInclude = new ArrayList<>();
					ArrayList<String> tempExclude = new ArrayList<>();

					if (annotation.toString().contains("includesResource")) { // If include resource then add the value in Include list
						tempInclude.add(value.toString());
					} else {
						tempExclude.add(value.toString());
					}
					
					//Update the Map with include and exclude resources
					resMap.put("<https://lpbenchgen.org/property/includesResource>", tempInclude); 
					resMap.put("<https://lpbenchgen.org/property/excludesResource>", tempExclude);

					mapOfData.put(subject.toString(), resMap); // Add the map for learning problem
					
				} else {// Entry already present in the map
					
					if (annotation.toString().contains("includesResource")) {// If include resource then update the value in Include list
						ArrayList<String> tempInclude = mapOfData.get(subject.toString()).get("<https://lpbenchgen.org/property/includesResource>");
						tempInclude.add(value.toString());
						
					} else {// If exclude resource then update the value in Exclude list
						ArrayList<String> tempExclude = mapOfData.get(subject.toString()).get("<https://lpbenchgen.org/property/excludesResource>");
						tempExclude.add(value.toString());
					}
					
				}
				
				
				//System.out.println("OWL Annotation Value : " + value);
				//System.out.println("OWL Annotation Subject : " + subject);
				//System.out.println("OWL Annotation : " + annotation);
				//System.out.println("OWL Annotation Property : " + annotationPropertiesInSignature);

			}

		};
		walker.walkStructure(visitor);

		//Sample code for reasoner
		//OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		//OWLReasoner reasoner = reasonerFactory.createReasoner(o);
		//reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
		
		//Sample code to fetch learning problems
		//Set<OWLNamedIndividual> instances = reasoner.getInstances(clas, false).entities().collect(Collectors.toSet());
		//System.out.println("Named Individuals : " + instances);
		
		return mapOfData;

	}
}
