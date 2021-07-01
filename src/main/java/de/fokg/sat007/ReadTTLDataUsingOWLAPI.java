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
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

/** This class reads data from the TTL file and generates separate files for each LP. 
 * @author Atul
 *
 */
public class ReadTTLDataUsingOWLAPI {
	
	//File Details
	static String trainFile; // train file name
	static String gradeFile; // Grading file name
	static String gradingFilePath; // File Path for grading data
	static String trainFilePath; // File Path for training data
	
	//URI Details
	static String resourceURI = "https://lpbenchgen.org/resource/"; // Resource URI
	static String includeResourceURI = "<https://lpbenchgen.org/property/includesResource>";
	static String excludeResourceURI = "<https://lpbenchgen.org/property/excludesResource>";
	static String baseURI = "http://dl-learner.org/carcinogenesis#";
	
	/**
	 * This method gets parameters from config file
	 * @throws IOException 
	 */
	public static void initializeParameters() throws IOException {
		Configuration config=new Configuration();
		trainFile = config.getPropertyValue("fokg.train.filename");
		gradeFile = config.getPropertyValue("fokg.grade.filename");
		gradingFilePath = config.getPropertyValue("fokg.grade.filepath");
		trainFilePath = config.getPropertyValue("fokg.train.filepath"); 
	}

	public static void main(String[] args) throws OWLOntologyCreationException, IOException {
		
		//Initialize parameters
		initializeParameters();
		
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		
		// Load train and grade file
		OWLOntology ontoTrain = m.loadOntologyFromOntologyDocument(new File(trainFile)); 
		OWLOntology ontoGrade = m.loadOntologyFromOntologyDocument(new File(gradeFile));
		System.out.println("Train and Grade Ontology loaded.");
		
		// Get data in Hash Map for train file
		HashMap<String, HashMap<String, ArrayList<String>>> mapOfDataTrain =	readData(ontoTrain);
		//System.out.println("Size of Map : " + mapOfData.size());
		
		//Get grading data in Map
		HashMap<String, HashMap<String, ArrayList<String>>> mapOfDataGrading = readData(ontoGrade);
		//System.out.println("Size of Map : " + mapOfDataTest.size());


		deleteOldFiles(mapOfDataTrain, trainFilePath); //Delete old files if any
		createNewFiles(mapOfDataTrain, trainFilePath); //Create files as per train data in HashMap
		
		deleteOldFiles(mapOfDataGrading, gradingFilePath); //Delete old files if any
		createNewFiles(mapOfDataGrading, gradingFilePath);  //Create files as per grading data in Map
		System.out.println("Train and Grade LP files created.");
	}
	
	
	/** This method creates files for LPs.
	 * @param mapOfData - Map containing LP details. 
	 * Key is LP URI and Value is another map. For Value map, Key is include/exclude URI and Value is list of carcinogenesis URI. 
	 * @param filePath - path where the generated files must be stored.
	 */
	private static void createNewFiles(HashMap<String, HashMap<String, ArrayList<String>>> mapOfData, String filePath) {
		Set<String> lpKeys = mapOfData.keySet();//Get Learning Problem Keys
		for(String lpKey: lpKeys ) {
			String fileName = lpKey.replaceAll(resourceURI, "");
			
			//Create positive and negative files for each learning problem
			File file = new File(filePath + fileName + "_p.txt");
			File file1 = new File(filePath + fileName + "_n.txt");
			
			try {
				file.createNewFile();
				file1.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Issue while creating a file");
			}
			
			try {
				//Write data for include resources
				FileOutputStream fileOut = new FileOutputStream(file);
				ArrayList<String> listIncludeResources = mapOfData.get(lpKey).get(includeResourceURI);
				for(String pos: listIncludeResources) {
					pos= pos.replaceAll(baseURI, "\"kb:")  + "\"," + "\n";
					fileOut.write(pos.getBytes());
				}
				fileOut.close();

				//Write data for exclude resources
				FileOutputStream fileOut1 = new FileOutputStream(file1);
				ArrayList<String> mapExcludeResources = mapOfData.get(lpKey).get(excludeResourceURI);
				for(String neg: mapExcludeResources) {
					neg= neg.replaceAll(baseURI, "\"kb:")  + "\"," +"\n";
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
		
	/** This method deletes old files for LPs.
	 * @param mapOfData - Map containing LP details. 
	 */
	private static void deleteOldFiles(HashMap<String, HashMap<String, ArrayList<String>>> mapOfData, String filePath) {
		Set<String> lpKeys = mapOfData.keySet();//Get Learning Problem Keys
		for(String lpKey: lpKeys ) {
			String fileName = lpKey.replaceAll(resourceURI, "");
			File file = new File(filePath + fileName + "_p.txt");
			File file1 = new File(filePath + fileName + "_n.txt");
			file.delete();
			file1.delete();
		}
	}

	/** This method reads LP from input ontology and creates a map for those LPs. 
	 * @param o - input Ontology.
	 * @return Map containing LP details. 
	 */
	private static HashMap<String, HashMap<String, ArrayList<String>>> readData(OWLOntology o) {
		
		// Initialize Map
		final HashMap<String, HashMap<String, ArrayList<String>>> mapOfData = new HashMap<>();

		//Walker to get the values from file.
		OWLOntologyWalker walker = new OWLOntologyWalker(Collections.singleton(o));
		OWLOntologyWalkerVisitor visitor = new OWLOntologyWalkerVisitor(walker) {

			public void visit(OWLAnnotationAssertionAxiom axiom) {
				OWLAnnotationValue value = axiom.getValue(); // chemical element
				OWLAnnotationSubject subject = axiom.getSubject();// learning problem id
				OWLAnnotation annotation = axiom.getAnnotation();// property either include or exclude 
				
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
					resMap.put(includeResourceURI, tempInclude); 
					resMap.put(excludeResourceURI, tempExclude);

					mapOfData.put(subject.toString(), resMap); // Add the map for learning problem
					
				} else {// Entry already present in the map
					
					if (annotation.toString().contains("includesResource")) {// If include resource then update the value in Include list
						ArrayList<String> tempInclude = mapOfData.get(subject.toString()).get(includeResourceURI);
						tempInclude.add(value.toString());
						
					} else {// If exclude resource then update the value in Exclude list
						ArrayList<String> tempExclude = mapOfData.get(subject.toString()).get(excludeResourceURI);
						tempExclude.add(value.toString());
					}
					
				}
				

			}

		};
		walker.walkStructure(visitor);
		
		return mapOfData;

	}
}
