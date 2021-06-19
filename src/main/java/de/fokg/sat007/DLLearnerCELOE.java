package de.fokg.sat007;

import java.io.File;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.compress.archivers.sevenz.CLI;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.owl.fuzzydll.FuzzyIndividual;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;

import edu.berkeley.compbio.jlibsvm.regression.RegressionCrossValidationResults;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

public class DLLearnerCELOE {
	// static File familyExamplesDir = new File("../examples");
	static String uriPrefix = "http://dl-learner.org/carcinogenesis#";

	public static void main(String[] args) throws ComponentInitException {
		/*
		 * Define the knowledge source > ks.type = "OWL File" > ks.fileName =
		 * "father.owl"
		 */
	    
		OWLFile ks = new OWLFile();
		ks.setFileName("carcinogenesis.owl");
		ks.init();
		

		/*
		 * Set up the reasoner > reasoner.type = "closed world reasoner" >
		 * reasoner.sources = { ks }
		 */
		ClosedWorldReasoner reasoner = new ClosedWorldReasoner();

		// create { ks }, i.e. a set containing ks
		Set<KnowledgeSource> sources = new HashSet<>();
		sources.add(ks);

		reasoner.setSources(sources);
		reasoner.init();
		
		

		/*
		 * Set up the learning problem > lp.type = "posNegStandard" >
		 * lp.positiveExamples = { "ex:stefan", "ex:markus", "ex:martin" } >
		 * lp.negativeExamples = { "ex:heinz", "ex:anna", "ex:michelle" }
		 */
		PosNegLPStandard lp = new PosNegLPStandard(reasoner);

		Scanner sc = null;
		HashSet<OWLIndividual> posExample = new HashSet<>();
		HashSet<OWLIndividual> negExample = new HashSet<>();
		try {
			for (int i = 1; i <= 20; i++) {
				sc = new Scanner(new File("lpfiles/lp_" + i + "_p.txt")); // lp_1_p.txt => i=1
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					posExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}
			}

			for (int i = 1; i <= 20; i++) {
				sc = new Scanner(new File("lpfiles/lp_"+i+"_n.txt"));
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					negExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Not able to read the file");
		}
		sc.close();

		lp.setPositiveExamples(posExample);
		lp.setNegativeExamples(negExample);

		lp.init();
		
		

		/*
		 * Set up the learning algorithm > alg.type = "celoe" >
		 * alg.maxExecutionTimeInSeconds = 1
		 */
		CELOE alg = new CELOE();
		alg.setMaxExecutionTimeInSeconds(120);

		// This 'wiring' is not part of the configuration file since it is
		// done automatically when using bin/cli. However it has to be done explicitly,
		// here.
		alg.setLearningProblem(lp);
		alg.setReasoner(reasoner);
		alg.setWriteSearchTree(true);
		alg.setSearchTreeFile("output.txt");
		

		alg.init();

		alg.start();
		int classExpressionTests = alg.getClassExpressionTests();
		System.out.println(classExpressionTests);
		OWLClassExpression currentlyBestDescription = alg.getCurrentlyBestDescription();
		System.out.println("Best Class Exp : " + currentlyBestDescription.toString());
		List<OWLClassExpression> currentlyBestDescriptions = alg.getCurrentlyBestDescriptions();
		System.out.println("Different class Exp : ");
		for(OWLClassExpression expression: currentlyBestDescriptions) {
			System.out.println("Class Exp  : " + expression.toString());
		}
		//alg.setNoisePercentage(30.0);
		
		// not Carbon
		
		checkTestData(currentlyBestDescription, reasoner);
		
		
		
		System.out.println();
	}
	
	public static void checkTestData(OWLClassExpression currentlyBestDescription, ClosedWorldReasoner reasoner) {
		
		// Get Test data
		Scanner sc = null;
		HashSet<OWLIndividual> posExample = new HashSet<>();
		HashSet<OWLIndividual> negExample = new HashSet<>();
		try {
			for (int i = 21; i < 26; i++) {
				sc = new Scanner(new File("lpfiles/lp_" + i + "_p.txt")); // lp_1_p.txt => i=1
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					posExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}
			}

			for (int i = 21; i < 26; i++) {
				sc = new Scanner(new File("lpfiles/lp_"+i+"_n.txt"));
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					negExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Not able to read the file");
		}
		sc.close();
		
		SortedSet<OWLClassExpression> equivalentClasses = reasoner.getEquivalentClasses(currentlyBestDescription);
		SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(currentlyBestDescription);
		SortedSet<FuzzyIndividual> fuzzyIndividuals = reasoner.getFuzzyIndividuals(currentlyBestDescription);
		SortedSet<OWLClassExpression> subClasses = reasoner.getSubClasses(currentlyBestDescription);
		
		System.out.println(equivalentClasses);
		System.out.println(individuals);
		System.out.println(fuzzyIndividuals);
		System.out.println(subClasses);
		
		
		Iterator<OWLIndividual> iterator = posExample.iterator();
		int pos_count = 0;
		while(iterator.hasNext()) {
			OWLEntity entity1 = (OWLEntity) iterator.next();
			boolean containsEntityInSignature = currentlyBestDescription.containsEntityInSignature(entity1);
			if(containsEntityInSignature) {
				pos_count++;
			}
		}
		
		System.out.println("POsitive Samples : " + posExample);
		System.out.println("POsitive Count : " + pos_count);
		System.out.println("Number of positive samples : " + posExample.size());
		
		iterator = negExample.iterator();
		int neg_count = 0;
		while(iterator.hasNext()) {
			OWLEntity entity1 = (OWLEntity) iterator.next();
			boolean containsEntityInSignature = currentlyBestDescription.containsEntityInSignature(entity1);
			if(!containsEntityInSignature) {
				neg_count++;
			}
		}
		
		System.out.println("Negative Samples : " + negExample);
		System.out.println("Negative Count : " + neg_count);
		System.out.println("Number of negative samples : " + negExample.size());
		
	}
}
