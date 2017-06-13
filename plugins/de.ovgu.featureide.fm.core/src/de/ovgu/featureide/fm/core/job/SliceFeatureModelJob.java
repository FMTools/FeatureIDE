/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.core.job;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.CNFCreator;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.analysis.cnf.manipulator.remove.CNFSlicer;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.SimpleSatSolver;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.impl.FMFormatManager;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.fm.core.job.util.JobArguments;

/**
 * Create mpl interfaces.
 * 
 * @author Sebastian Krieter
 * @author Marcus Pinnecke (Feature Interface)
 */
public class SliceFeatureModelJob implements LongRunningMethod<IFeatureModel> {

	public static class Arguments implements JobArguments<IFeatureModel> {
		private final boolean considerConstraints;
		private final IFeatureModel featuremodel;
		private final Collection<String> featureNames;
		private final Path modelFile;

		public Arguments(Path modelFile, IFeatureModel featuremodel, Collection<String> featureNames, boolean considerConstraints) {
			this.modelFile = modelFile;
			this.featuremodel = featuremodel;
			this.featureNames = featureNames;
			this.considerConstraints = considerConstraints;
		}

		@Override
		public SliceFeatureModelJob createJob() {
			return new SliceFeatureModelJob(modelFile, featuremodel, featureNames, considerConstraints);
		}
	}

	private static final int GROUP_OR = 1, GROUP_AND = 2, GROUP_ALT = 3, GROUP_NO = 0;
	private static final String MARK1 = "?", MARK2 = "??";

	private boolean changed = false;
	private final boolean considerConstraints;
	private final IFeatureModel featureModel;
	private final Collection<String> featureNames;

	private final Path modelFile;

	private IFeatureModel newInterfaceModel = null;

	public SliceFeatureModelJob(Path modelFile, IFeatureModel featuremodel, Collection<String> featureNames, boolean considerConstraints) {
		this.modelFile = modelFile;
		this.featureModel = featuremodel;
		this.featureNames = featureNames;
		this.considerConstraints = considerConstraints;
	}

	@Override
	public IFeatureModel execute(IMonitor monitor) throws Exception {
		newInterfaceModel = sliceModel(monitor);
		saveModel();
		return newInterfaceModel;
	}

	public IFeatureModel getInterfaceModel() {
		return newInterfaceModel;
	}

	// TODO Change to own job
	public IFeatureModel sliceModel(IMonitor monitor) {
		final IFeatureModelFactory factory = FMFactoryManager.getFactory(featureModel);
		monitor.setTaskName("Slicing Feature Model");
		monitor.setRemainingWork(100);

		monitor.checkCancel();
		final CNF cnf = sliceFormula(monitor.subTask(80));
		monitor.checkCancel();
		final IFeatureModel m = sliceTree(featureNames, featureModel, factory, monitor.subTask(2));
		monitor.checkCancel();
		merge(factory, cnf, m, monitor.subTask(18));

		return m;
	}

	private boolean cut(final IFeature curFeature) {
		final IFeatureStructure structure = curFeature.getStructure();
		boolean notSelected = curFeature.getName().equals(MARK1);

		List<IFeature> list = FeatureUtils.convertToFeatureList(structure.getChildren());
		if (list.isEmpty()) {
			return notSelected;
		} else {
			boolean[] remove = new boolean[list.size()];
			int removeCount = 0;

			int i = 0;
			for (IFeature child : list) {
				remove[i++] = cut(child);
			}

			// remove children
			Iterator<IFeature> it = list.iterator();
			for (i = 0; i < remove.length; i++) {
				IFeature feat = it.next();
				if (remove[i]) {
					it.remove();
					feat.getStructure().getParent().removeChild(feat.getStructure());
					feat.getStructure().setParent(null);
					removeCount++;
					//    				changed = true;
				}
			}
			if (list.isEmpty()) {
				structure.setAnd();
				return notSelected;
			} else {
				switch (getGroup(structure)) {
				case GROUP_OR:
					if (removeCount > 0) {
						structure.setAnd();
						for (IFeature child : list) {
							child.getStructure().setMandatory(false);
						}
					} else if (list.size() == 1) {
						structure.setAnd();
						for (IFeature child : list) {
							child.getStructure().setMandatory(true);
						}
					}
					break;
				case GROUP_ALT:
					if (removeCount > 0) {
						if (list.size() == 1) {
							structure.setAnd();
							for (IFeature child : list) {
								child.getStructure().setMandatory(false);
							}
						} else {
							final IFeatureModel featureModel = curFeature.getFeatureModel();
							IFeature pseudoAlternative = FMFactoryManager.getFactory(featureModel).createFeature(featureModel, MARK2);
							pseudoAlternative.getStructure().setMandatory(false);
							pseudoAlternative.getStructure().setAlternative();
							for (IFeature child : list) {
								pseudoAlternative.getStructure().addChild(child.getStructure());
								structure.removeChild(child.getStructure());
							}
							list.clear();
							structure.setAnd();
							structure.addChild(pseudoAlternative.getStructure());
						}
					} else if (list.size() == 1) {
						structure.setAnd();
						for (IFeature child : list) {
							child.getStructure().setMandatory(true);
						}
					}
					break;
				}
			}
		}
		return false;
	}

	private void deleteFeature(IFeatureStructure curFeature) {
		IFeatureStructure parent = curFeature.getParent();
		List<IFeatureStructure> children = curFeature.getChildren();
		parent.removeChild(curFeature);
		changed = true;
		for (IFeatureStructure child : children) {
			parent.addChild(child);
		}
		children.clear();// XXX code smell
	}

	private int getGroup(IFeatureStructure f) {
		if (f == null) {
			return GROUP_NO;
		} else if (f.isAnd()) {
			return GROUP_AND;
		} else if (f.isOr()) {
			return GROUP_OR;
		} else {
			return GROUP_ALT;
		}
	}

	private void merge(IFeatureModelFactory factory, CNF cnf, IFeatureModel m, IMonitor monitor) {
		final List<LiteralSet> children = cnf.getClauses();

		monitor.setTaskName("Adding Constraints");
		monitor.setRemainingWork(children.size() + 1);

		final SimpleSatSolver s = new SimpleSatSolver(CNFCreator.createNodes(m));
		monitor.step();

		for (LiteralSet clause : children) {
			switch (s.hasSolution(clause.negate().getLiterals())) {
			case FALSE:
				break;
			case TIMEOUT:
			case TRUE:
				m.addConstraint(factory.createConstraint(m, Nodes.convert(cnf.getVariables(), clause)));
				break;
			default:
				assert false;
			}
			monitor.step();
		}
	}

	private void merge(IFeatureStructure curFeature, int parentGroup) {
		if (!curFeature.hasChildren()) {
			return;
		}
		int curFeatureGroup = getGroup(curFeature);
		LinkedList<IFeatureStructure> list = new LinkedList<>(curFeature.getChildren());
		try {
			for (IFeatureStructure child : list) {
				merge(child, curFeatureGroup);
				curFeatureGroup = getGroup(curFeature);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (curFeature.getFeature().getName().equals(MARK1)) {
			if (parentGroup == curFeatureGroup) {
				if (parentGroup == GROUP_AND && !curFeature.isMandatory()) {
					for (IFeatureStructure feature : curFeature.getChildren()) {
						feature.setMandatory(false);
					}
				}
				deleteFeature(curFeature);
			} else {
				switch (parentGroup) {
				case GROUP_AND:
					IFeatureStructure parent = curFeature.getParent();
					if (parent.getChildrenCount() == 1) {
						switch (curFeatureGroup) {
						case GROUP_OR:
							parent.setOr();
							break;
						case GROUP_ALT:
							parent.setAlternative();
							break;
						}
						deleteFeature(curFeature);
					}
					break;
				case GROUP_OR:
					if (curFeatureGroup == GROUP_AND) {
						boolean allOptional = true;
						for (IFeatureStructure child : list) {
							if (child.isMandatory()) {
								allOptional = false;
								break;
							}
						}
						if (allOptional) {
							deleteFeature(curFeature);
						}
					}
					break;
				case GROUP_ALT:
					if (curFeatureGroup == GROUP_AND && list.size() == 1) {
						deleteFeature(curFeature);
					}
					break;
				}
			}
		}
	}

	private void saveModel() {
		final Path filePath = modelFile.getFileName();
		final Path root = modelFile.getRoot();
		if (filePath != null && root != null) {
			String fileName = filePath.toString();
			final int extIndex = fileName.lastIndexOf('.');
			fileName = (extIndex > 0) ? fileName.substring(0, extIndex) + "_sliced_" + System.currentTimeMillis() + ".xml"
					: fileName + "_sliced_" + System.currentTimeMillis() + ".xml";
			final Path outputPath = root.resolve(modelFile.subpath(0, modelFile.getNameCount() - 1)).resolve(fileName);

			FileHandler.save(outputPath, newInterfaceModel, FMFormatManager.getInstance().getFormatByFileName(fileName));
		}
	}

	private CNF sliceFormula(IMonitor monitor) {
		monitor.setTaskName("Slicing Feature Model Formula");
		final ArrayList<String> removeFeatures = new ArrayList<>(FeatureUtils.getFeatureNames(featureModel));
		removeFeatures.removeAll(featureNames);
		final CNF satInstance = CNFCreator.createNodes(featureModel);
		final CNF sliced = LongRunningWrapper.runMethod(new CNFSlicer(satInstance, removeFeatures), monitor.subTask(1));
		return sliced;
	}

	private IFeatureModel sliceTree(Collection<String> selectedFeatureNames, IFeatureModel orgFeatureModel, IFeatureModelFactory factory, IMonitor monitor) {
		monitor.setTaskName("Slicing Feature Tree");
		monitor.setRemainingWork(2);
		final IFeatureModel m = orgFeatureModel.clone();
		// mark features
		for (IFeature feat : m.getFeatures()) {
			if (!selectedFeatureNames.contains(feat.getName())) {
				feat.setName(MARK1);
			}
		}

		IFeature root = m.getStructure().getRoot().getFeature();

		m.getStructure().setRoot(null);
		m.reset();

		// set new abstract root
		IFeature nroot = factory.createFeature(m, "__root__");
		nroot.getStructure().setAbstract(true);
		nroot.getStructure().setAnd();
		nroot.getStructure().addChild(root.getStructure());
		root.getStructure().setParent(nroot.getStructure());

		// merge tree
		cut(nroot);
		do {
			changed = false;
			merge(nroot.getStructure(), GROUP_NO);
		} while (changed);
		monitor.step();

		int count = 0;
		Hashtable<String, IFeature> featureTable = new Hashtable<String, IFeature>();
		LinkedList<IFeature> featureStack = new LinkedList<IFeature>();
		featureStack.push(nroot);
		while (!featureStack.isEmpty()) {
			IFeature curFeature = featureStack.pop();
			for (IFeature feature : FeatureUtils.convertToFeatureList(curFeature.getStructure().getChildren())) {
				featureStack.push(feature);
			}
			if (curFeature.getName().startsWith(MARK1)) {
				curFeature.setName("_Abstract_" + count++);
				curFeature.getStructure().setAbstract(true);
			}
			featureTable.put(curFeature.getName(), curFeature);
		}
		m.setFeatureTable(featureTable);
		m.getStructure().setRoot(nroot.getStructure());

		if (considerConstraints) {
			final ArrayList<IConstraint> innerConstraintList = new ArrayList<>();
			for (IConstraint constaint : orgFeatureModel.getConstraints()) {
				final Collection<IFeature> containedFeatures = constaint.getContainedFeatures();
				boolean containsAllfeatures = !containedFeatures.isEmpty();
				for (IFeature feature : containedFeatures) {
					if (!selectedFeatureNames.contains(feature.getName())) {
						containsAllfeatures = false;
						break;
					}
				}
				if (containsAllfeatures) {
					innerConstraintList.add(constaint);
				}
			}
			for (IConstraint constraint : innerConstraintList) {
				m.addConstraint(constraint.clone(m));
			}
		}
		monitor.step();

		return m;
	}

}
