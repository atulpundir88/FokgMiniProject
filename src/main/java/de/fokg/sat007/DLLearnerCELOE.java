package de.fokg.sat007;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * This class uses DLLearner CELOE algorithm to generate class expressions and
 * classify individuals as true or false.
 * 
 * @author Atul
 *
 */
public class DLLearnerCELOE {

	static HashMap<String, Integer> countPositive = new HashMap<>();

	static String uriPrefix = "http://dl-learner.org/carcinogenesis#";

	static String gradingFilePath; // File Path for grading data
	static String trainFilePath; // File Path for training data
	static String trainOutputFile; // Output File for training data
	static String gradeOutputFile; // Output File for grade data

	// LP parameters
	static int startLPTrain; // First LP number for train
	static int totalLPsTrain; // Total number of LPs for train
	static int startLPGrade; // First LP number for grade
	static int totalLPsGrade; // Total number of LPs for grade

	static boolean executeTrain; // Should be if need to execute algorithm for train data

	/**
	 * This method gets parameters from config file
	 * 
	 * @throws IOException
	 */
	public static void initializeParameters() throws IOException {
		Configuration config = new Configuration();

		gradingFilePath = config.getPropertyValue("fokg.grade.filepath");
		trainFilePath = config.getPropertyValue("fokg.train.filepath");
		trainOutputFile = config.getPropertyValue("fokg.train.output.filedetail");
		gradeOutputFile = config.getPropertyValue("fokg.grade.output.filedetail");

		startLPTrain = Integer.parseInt(config.getPropertyValue("fokg.train.startlpnumber"));
		totalLPsTrain = Integer.parseInt(config.getPropertyValue("fokg.train.totallps"));
		executeTrain = Boolean.parseBoolean(config.getPropertyValue("fokg.train.execute"));

		startLPGrade = Integer.parseInt(config.getPropertyValue("fokg.grade.startlpnumber"));
		totalLPsGrade = Integer.parseInt(config.getPropertyValue("fokg.grade.totallps"));

	}

	public static void main(String[] args) throws ComponentInitException, OWLOntologyCreationException, IOException {

		long start = System.currentTimeMillis(); // Initialize start time.

		initializeParameters();

		// Invoke Algorithm for train data
		if (executeTrain) {
			HashMap<Integer, SortedSet<OWLIndividual>> trainResults = invokeAlgo(trainFilePath, startLPTrain,
					totalLPsTrain, true);
			createOutputFile(trainResults, trainOutputFile);
		}

		// Invoke Algorithm for test data
		HashMap<Integer, SortedSet<OWLIndividual>> gradeResults = invokeAlgo(gradingFilePath, startLPGrade,
				totalLPsGrade, false);
		createOutputFile(gradeResults, gradeOutputFile);

		long end = System.currentTimeMillis();
		System.out.println("Total time : " + (end - start));
	}

	/**
	 * This method invokes the CELOE algorithm with below parameters.
	 * 
	 * @param filePath
	 *            - file path containing LP files
	 * @param startLP
	 *            - start LP number.
	 * @param totalNumberOfLP
	 *            - Total number of LPs.
	 * @param alg
	 *            - CELOE algorithm object.
	 * @param lp
	 *            - PosOnlyLP learning problem object.
	 * @param reasoner
	 *            - Reasoner object.
	 * @param trainMode
	 *            - boolean should be true for train file else false.
	 * @throws ComponentInitException
	 */
	private static HashMap<Integer, SortedSet<OWLIndividual>> invokeAlgo(String filePath, int startLP,
			int totalNumberOfLP, boolean trainMode) throws ComponentInitException {

		
		Scanner sc = null; // Initialize scanner to read data from file
		HashMap<Integer, SortedSet<OWLIndividual>> results = new HashMap<>();

		int lp_no = startLP;
		int lp_end = startLP + totalNumberOfLP;

		while (lp_no < lp_end) {
			System.out.println("LP no - " + lp_no);

			HashSet<OWLIndividual> posExample = new HashSet<>(); // Map containing positive examples
			HashSet<OWLIndividual> negExample = new HashSet<>(); // Map containing negative examples
			HashSet<OWLIndividual> posExampleRemovedElement = new HashSet<>(); // Temp Map for train data to store 80%
																				// of data

			try {
				// Read positive examples
				sc = new Scanner(new File(filePath + "lp_" + lp_no + "_p.txt"));
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					posExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}

				// Read negative examples
				sc = new Scanner(new File(filePath + "lp_" + lp_no + "_n.txt"));
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					negExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}

				// Consider 80% of data for training
				if (trainMode) {
					int index = 0;
					for (OWLIndividual posElement : posExample) {
						posExampleRemovedElement.add(posElement);
						index++;
						if (index > (posExample.size() * 0.8))
							break;
					}
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("Not able to read the file");
			}
			sc.close();
			
			// Load carcinogenesis.owl file
			OWLFile ks = new OWLFile();
			ks.setFileName("carcinogenesis.owl");
			ks.init();

			// Initialize Reasoner
			ClosedWorldReasoner reasoner = new ClosedWorldReasoner();

			// Initialize knowledge sources
			Set<KnowledgeSource> sources = new HashSet<>();
			sources.add(ks);
			reasoner.setSources(sources);
			reasoner.init();

			// Initialize CELOE object
			CELOE alg = new CELOE();

			// Initialize PosOnlyLP object
			PosOnlyLP lp = new PosOnlyLP(reasoner);


			// Set learning problem
			if (trainMode) {
				lp.setPositiveExamples(posExampleRemovedElement);
			} else {
				lp.setPositiveExamples(posExample);
			}

			lp.init();

			// Set CELOE algorithm parameters
			alg.setMaxExecutionTimeInSeconds(30); // 30s execution time for each LP
			alg.setLearningProblem(lp);
			alg.setReasoner(reasoner);
			alg.init();
			alg.start();

			// Get best class expression
			OWLClassExpression currentlyBestDescription = alg.getCurrentlyBestDescription();
			SortedSet<OWLIndividual> positiveClassified = reasoner.getIndividuals(currentlyBestDescription);
			results.put(lp_no, positiveClassified);

			lp_no++;
			System.out.println("--------------END----------------");

		}

		return results;

	}

	

	/**
	 * This method creates output file for all LPs
	 * 
	 * @param results
	 *            - map containing positive examples for lp.
	 * @param fileDetails
	 *            - output file details.
	 * @param reasoner
	 *            - reasoner object.
	 * @throws IOException
	 * @throws ComponentInitException 
	 */
	private static void createOutputFile(HashMap<Integer, SortedSet<OWLIndividual>> results, String fileDetails) throws IOException, ComponentInitException {

		// Load carcinogenesis.owl file
		OWLFile ks = new OWLFile();
		ks.setFileName("carcinogenesis.owl");
		ks.init();

		// Initialize Reasoner
		ClosedWorldReasoner reasoner = new ClosedWorldReasoner();

		// Initialize knowledge sources
		Set<KnowledgeSource> sources = new HashSet<>();
		sources.add(ks);
		reasoner.setSources(sources);
		reasoner.init();

		String prefix = "@prefix carcinogenesis: <http://dl-learner.org/carcinogenesis#> .\r\n"
				+ "@prefix lpres: <https://lpbenchgen.org/resource/> .\r\n"
				+ "@prefix lpprop: <https://lpbenchgen.org/property/> .\r\n" + "\r\n";

		String lpStatement = "";
		int resultIndex = 1;
		for (Integer key : results.keySet()) {
			String trueStatement = "lpres:result_" + resultIndex + "pos lpprop:belongsToLP true ;\r\n"
					+ "    lpprop:pertainsTo lpres:lp_" + key + " ;\r\n";

			SortedSet<OWLIndividual> positiveExamples = results.get(key);
			SortedSet<OWLIndividual> allIndividuals = reasoner.getIndividuals();
			allIndividuals.removeAll(positiveExamples);

			String positiveElement = "    lpprop:resource";
			for (OWLIndividual element : positiveExamples) {
				positiveElement = positiveElement + " carcinogenesis:" + element.toString() + ",";
			}
			positiveElement = positiveElement.substring(0, positiveElement.length() - 1) + ".\r\n" + "\r\n";

			trueStatement = trueStatement + positiveElement;

			String falseStatement = "lpres:result_" + resultIndex + "neg lpprop:belongsToLP false ;\r\n"
					+ "    lpprop:pertainsTo lpres:lp_" + key + " ;\r\n";

			String negativeElement = "    lpprop:resource";
			for (OWLIndividual element : allIndividuals) {
				negativeElement = negativeElement + " carcinogenesis:" + element.toString() + ",";
			}
			negativeElement = negativeElement.substring(0, negativeElement.length() - 1) + ".\r\n" + "\r\n";
			falseStatement = falseStatement + negativeElement;

			lpStatement = lpStatement + trueStatement + falseStatement;
			resultIndex++;

		}

		OntModel ontModel = ModelFactory.createOntologyModel();
		ontModel.read(new StringReader(prefix + lpStatement), null, "TTL");
		ontModel.write(new FileWriter(fileDetails), "TTL");

	}

}