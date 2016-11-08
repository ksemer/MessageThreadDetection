package utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

import edu.cmu.lti.ws4j.RelatednessCalculator;
import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Email;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class WekaUtil {

	public static Instances creteWekaEmailRandomPairsInstances(ArrayList<Email> emails, ArrayList<String> features,
			int numInstSameThread, int numInstDifferentThread, long randomSeed, EmailSimGlobalCloseness esimClosenesses,
			RelatednessCalculator wordnet) {
		ArrayList<Attribute> atts = new ArrayList<>();
		ArrayList<String> nominalAtt = new ArrayList<>();
		nominalAtt.add("different");
		nominalAtt.add("same");
		atts.add(new Attribute("Emails", (FastVector) null));
		for (String feat : features) {
			atts.add(new Attribute(feat));
		}
		atts.add(new Attribute("samethread", nominalAtt));

		Instances wekainst = new Instances("weka_pairs", atts, 0);

		try {
			// Preparation for java reflection
			Class[] type = { EmailSimGlobalCloseness.class, ArrayList.class, RelatednessCalculator.class };
			Class cls_comparator = Class.forName("processing.text.enron.textproc.EmailsComparator");
			Constructor cons = cls_comparator.getConstructor(type);
			Object[] const_param = { esimClosenesses, emails, wordnet };
			Object obj_comparator = cons.newInstance(const_param);
			Class[] param = new Class[2];
			param[0] = Email.class;
			param[1] = Email.class;
			Object[] arguments = new Email[2];

			Random random = new Random(randomSeed);
			int size = emails.size();
			System.out.print("|");
			for (int i = 0; i < size; i++) {
				if (i % (size / 30) == 0)
					System.out.print("-");
				Email ei = emails.get(i);
				int conInstSameThread = 0, contInstDifferentThread = 0;
				int sameThread = 0, differentThread = 0;
				for (int j = 0; j < emails.size(); j++) {
					if (i == j)
						continue;
					if (ei.getThreadid() == emails.get(j).getThreadid())
						sameThread++;
					else
						differentThread++;

				}
				if (sameThread == 0) {
					continue;
				}
				if (sameThread > numInstSameThread) {
					sameThread = numInstSameThread;
				}
				if (differentThread > numInstDifferentThread) {
					differentThread = numInstDifferentThread;
				}
				ArrayList<Integer> alreadyAdded = new ArrayList<>();
				alreadyAdded.add(ei.getId());
				while (conInstSameThread < sameThread || contInstDifferentThread < differentThread) {
					Email ej = emails.get(random.nextInt(size));
					if (alreadyAdded.contains(ej.getId()))
						continue;

					arguments[0] = ei;
					arguments[1] = ej;
					alreadyAdded.add(ej.getId());
					if (ei.getThreadid() == ej.getThreadid() && conInstSameThread < sameThread) {
						// same thread
						double[] vals = new double[wekainst.numAttributes()];
						vals[0] = wekainst.attribute(0).addStringValue("" + ei.getId() + "_" + ej.getId());
						int f = 1;
						for (String feat : features) {
							Method method = cls_comparator.getDeclaredMethod(feat, param);
							Double val = (Double) method.invoke(obj_comparator, arguments);
							if (val != null)
								vals[f] = val;
							else
								vals[f] = 0;
							f++;
						}
						vals[f] = nominalAtt.indexOf("same");
						wekainst.add(new SparseInstance(1, vals));
						conInstSameThread++;
					} else if (ei.getThreadid() != ej.getThreadid() && contInstDifferentThread < differentThread) {
						// different thread
						double[] vals = new double[wekainst.numAttributes()];
						vals[0] = wekainst.attribute(0).addStringValue(ei.getId() + "_" + ej.getId());
						int f = 1;
						for (String feat : features) {
							Method method = cls_comparator.getDeclaredMethod(feat, param);
							Double val = (Double) method.invoke(obj_comparator, arguments);
							if (val != null)
								vals[f] = val;
							else
								vals[f] = 0;
							f++;
						}
						vals[f] = nominalAtt.indexOf("different");
						wekainst.add(new SparseInstance(1, vals));
						contInstDifferentThread++;
					}
				}
			}

			System.out.println("|");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wekainst;
	}

	public static Instances creteWekaEmailPairsInstances(ArrayList<Email> emails, ArrayList<String> features,
			int numInstSameThread, int numInstDifferentThread, EmailSimGlobalCloseness esimClosenesses,
			RelatednessCalculator wordnet) {
		ArrayList<Attribute> atts = new ArrayList<>();
		ArrayList<String> nominalAtt = new ArrayList<>();
		nominalAtt.add("different");
		nominalAtt.add("same");
		atts.add(new Attribute("Emails", (FastVector) null));
		for (String feat : features) {
			atts.add(new Attribute(feat));
		}
		atts.add(new Attribute("samethread", nominalAtt));

		Instances wekainst = new Instances("weka_pairs", atts, 0);

		try {
			// Preparation for java reflection
			Class[] type = { EmailSimGlobalCloseness.class, ArrayList.class, RelatednessCalculator.class };
			Class cls_comparator = Class.forName("processing.text.enron.textproc.EmailsComparator");
			Constructor cons = cls_comparator.getConstructor(type);
			Object[] const_param = { esimClosenesses, emails, wordnet };
			Object obj_comparator = cons.newInstance(const_param);
			Class[] param = new Class[2];
			param[0] = Email.class;
			param[1] = Email.class;
			Object[] arguments = new Email[2];

			for (int i = 0; i < emails.size(); i++) {
				Email ei = emails.get(i);
				int conInstSameThread = 0, contInstDifferentThread = 0;
				for (int j = 0; j < emails.size(); j++) {
					if (i == j)
						continue;
					Email ej = emails.get(j);
					arguments[0] = ei;
					arguments[1] = ej;
					if (ei.getThreadid() == ej.getThreadid() && conInstSameThread < numInstSameThread) {
						// same thread
						double[] vals = new double[wekainst.numAttributes()];
						vals[0] = wekainst.attribute(0).addStringValue("" + ei.getId() + "_" + ej.getId());
						int f = 1;
						for (String feat : features) {

							Method method = cls_comparator.getDeclaredMethod(feat, param);
							Double val = (Double) method.invoke(obj_comparator, arguments);
							if (val != null)
								vals[f] = val;
							else
								vals[f] = 0;
							f++;
						}
						vals[f] = nominalAtt.indexOf("same");
						wekainst.add(new SparseInstance(1, vals));
						conInstSameThread++;
					} else if (ei.getThreadid() != ej.getThreadid()
							&& contInstDifferentThread < numInstDifferentThread) {
						// different thread
						double[] vals = new double[wekainst.numAttributes()];
						vals[0] = wekainst.attribute(0).addStringValue(ei.getId() + "_" + ej.getId());
						int f = 1;
						for (String feat : features) {

							Method method = cls_comparator.getDeclaredMethod(feat, param);
							Double val = (Double) method.invoke(obj_comparator, arguments);
							if (val != null)
								vals[f] = val;
							else
								vals[f] = 0;
							f++;
						}
						vals[f] = nominalAtt.indexOf("different");
						wekainst.add(new SparseInstance(1, vals));
						contInstDifferentThread++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wekainst;
	}

	public static Instances creteWekaEmailPairsSingleInstance(Email email1, Email email2, ArrayList<String> features,
			EmailSimGlobalCloseness esimClosenesses, ArrayList<Email> emails, RelatednessCalculator wordnet) {
		ArrayList<Attribute> atts = new ArrayList<>();
		ArrayList<String> nominalAtt = new ArrayList<>();
		nominalAtt.add("same");
		nominalAtt.add("different");
		atts.add(new Attribute("Emails", (FastVector) null));
		for (String feat : features) {
			atts.add(new Attribute(feat));
		}
		atts.add(new Attribute("samethread", nominalAtt));
		Instances wekainst = new Instances("weka_pairs", atts, 0);
		double[] vals = new double[wekainst.numAttributes()];

		try {
			// Preparation for java reflection
			Class[] type = { EmailSimGlobalCloseness.class, ArrayList.class, RelatednessCalculator.class };
			Class cls_comparator = Class.forName("processing.text.enron.textproc.EmailsComparator");
			Constructor cons = cls_comparator.getConstructor(type);
			Object[] const_param = { esimClosenesses, emails, wordnet };
			Object obj_comparator = cons.newInstance(const_param);
			Class[] param = new Class[2];
			param[0] = Email.class;
			param[1] = Email.class;
			Object[] arguments = new Email[2];
			arguments[0] = email1;
			arguments[1] = email2;

			vals[0] = atts.get(0).addStringValue("" + email1.getId() + "_" + email2.getId());
			int f = 1;
			for (String feat : features) {

				Method method = cls_comparator.getDeclaredMethod(feat, param);
				Double val = (Double) method.invoke(obj_comparator, arguments);
				if (val != null)
					vals[f] = val;
				else
					vals[f] = 0;
				f++;
			}
			if (email1.getThreadid() == email2.getThreadid())
				vals[f] = nominalAtt.indexOf("same");
			else
				vals[f] = nominalAtt.indexOf("different");

		} catch (Exception e) {
			e.printStackTrace();
		}
		wekainst.add(new SparseInstance(1, vals));
		return wekainst;
	}

	public static double[] convertInstanceToFeaturesVector(Instance inst) {
		double[] inst_vector = new double[inst.numAttributes() - 1];
		for (int a = 0; a < inst.numAttributes(); a++) {
			if (a != inst.classIndex())
				inst_vector[a] = inst.value(a);
		}
		return inst_vector;
	}

}
