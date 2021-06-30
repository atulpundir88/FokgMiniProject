package de.fokg.sat007;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.experiments.ExMakerCrossFolds;
import org.dllearner.experiments.Examples;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

public class DLLearnerCELOE {

	static String uriPrefix = "http://dl-learner.org/carcinogenesis#";

	public static void main(String[] args) throws ComponentInitException, OWLOntologyCreationException {

		long start = System.currentTimeMillis();

		OWLFile ks = new OWLFile();
		ks.setFileName("carcinogenesis.owl");
		ks.init();

		ClosedWorldReasoner reasoner = new ClosedWorldReasoner();

		Set<KnowledgeSource> sources = new HashSet<>();
		sources.add(ks);
		reasoner.setSources(sources);
		reasoner.init();

		Scanner sc = null;

		List<Double> listF1Score = new ArrayList<Double>();

		int lp_no = 26;

		CELOE alg = new CELOE();
		PosOnlyLP lp = new PosOnlyLP(reasoner);

		while (lp_no < 51) {
			System.out.println("LP no - " + lp_no);

			HashSet<OWLIndividual> posExample = new HashSet<>();
			HashSet<OWLIndividual> negExample = new HashSet<>();
			HashSet<OWLIndividual> posExampleRemovedElement = new HashSet<>();

			try {
				// for (int i = lp_no; i < 27; i++) {
				sc = new Scanner(new File("lpfiles/grading/lp_" + lp_no + "_p.txt")); //
				//sc = new Scanner(new File("lpfiles/lp_" + lp_no + "_p.txt"));
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					posExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}
				// }

				// for (int i = lp_no; i < 27; i++) {
				sc = new Scanner(new File("lpfiles/grading/lp_" + lp_no + "_n.txt"));
				//sc = new Scanner(new File("lpfiles/lp_" + lp_no + "_n.txt"));
				while (sc.hasNextLine()) {
					String iri = sc.nextLine().replaceAll("\"kb:", "").replaceAll("\",", "");
					negExample.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + iri)));
				}

				int index = 0;
				for (OWLIndividual posElement : posExample) {
					posExampleRemovedElement.add(posElement);
					index++;
					if (index > (posExample.size() * 0.8))
						break;
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("Not able to read the file");
			}
			sc.close();

			// PosNegLPStandard lp = new PosNegLPStandard(reasoner);
			lp.setPositiveExamples(posExample);
			lp.init();
			// lp.setNegativeExamples(negExampleRemovedElement);

			alg.setMaxExecutionTimeInSeconds(30);
			alg.setLearningProblem(lp);
			alg.setReasoner(reasoner);
			alg.init();
			// alg.setNoisePercentage(30); 
			alg.start();

			OWLClassExpression currentlyBestDescription = alg.getCurrentlyBestDescription();
			System.out.println("Best Class Exp : " + currentlyBestDescription.toString());
			Set<OWLClassExpression> temp = new HashSet<>();
			temp.add(currentlyBestDescription);
			
			//Validate F1Score
			/*double f1Score = validateExamples(posExample, negExample, temp, reasoner, true);
			listF1Score.add(f1Score);
			System.out.println("F1 Score : " + f1Score);*/
			
			Set<OWLIndividual> individuals = new HashSet<>();
			for (OWLClassExpression classExpression : temp) {
				SortedSet<OWLIndividual> individuals2 = reasoner.getIndividuals(classExpression);
				individuals.addAll(individuals2);
				System.out.println("LP : " + lp_no);
				System.out.println("Number of Positives : " + individuals2.size());
				System.out.println("Positive Individual(Given in LP) : " + posExample);
				System.out.println("Postivie Individual(Classified by Algo) : " + individuals2);
			}
			
			
			

			lp_no++;
			System.out.println("--------------END----------------");

		}

		System.out.println("F1-Scores : ");
		for (Double f1Score : listF1Score) {
			System.out.println(f1Score);
		}

		long end = System.currentTimeMillis();
		System.out.println("Total time : " + (end - start));
	}

	private static double validateExamples(HashSet<OWLIndividual> posExample, HashSet<OWLIndividual> negExample,
			Set<OWLClassExpression> currentlyBestDescription, ClosedWorldReasoner reasoner, boolean display) {

		Set<OWLIndividual> individuals = new HashSet<>();
		for (OWLClassExpression classExpression : currentlyBestDescription) {
			// SortedSet<OWLIndividual> individuals =
			// reasoner.getIndividuals(currentlyBestDescription);
			individuals.addAll(reasoner.getIndividuals(classExpression));
		}

		Iterator<OWLIndividual> iterator = posExample.iterator();
		int pos_count = 0;
		int pos_count_neg = 0;
		while (iterator.hasNext()) {
			OWLIndividual next = iterator.next();
			if (individuals.contains(next)) {
				pos_count++;
			} else {
				pos_count_neg++;
			}
		}

		// System.out.println("POsitive Samples : " + posExample);
		if (display) {
			System.out.println("POsitive Count : " + pos_count);
			System.out.println("POsitive Count (classified as negative) : " + pos_count_neg);
			System.out.println("Number of positive samples : " + posExample.size());
		}

		iterator = negExample.iterator();
		int neg_count = 0;
		int neg_count_pos = 0;
		while (iterator.hasNext()) {
			OWLIndividual next = iterator.next();
			if (!individuals.contains(next)) {
				neg_count++;
			} else {
				neg_count_pos++;
			}
		}

		// System.out.println("Negative Samples : " + negExample);
		if (display) {
			System.out.println("Negative Count : " + neg_count);
			System.out.println("Negative Count (classified as positive) : " + neg_count_pos);
			System.out.println("Number of negative samples : " + negExample.size());
		}

		return getF1Score(pos_count, neg_count, pos_count_neg, neg_count_pos);

	}

	private static double getF1Score(int tp, int tn, int fp, int fn) {
		double f1Score = 0.0;
		f1Score = (double) (2 * tp) / (2 * tp + fp + fn);
		return f1Score;
	}

}