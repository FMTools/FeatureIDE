/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations;

import static de.ovgu.featureide.fm.core.localization.StringTable.AVERAGE_FEATURES_PER_DIRECTIVE;
import static de.ovgu.featureide.fm.core.localization.StringTable.AVERAGE_NUMBER_OF_DIRECTIVES_PER_CLASS;
import static de.ovgu.featureide.fm.core.localization.StringTable.CLASS_STATISTICS;
import static de.ovgu.featureide.fm.core.localization.StringTable.DIRECTIVES_PER_CLASS;
import static de.ovgu.featureide.fm.core.localization.StringTable.FEATURES_PER_DIRECTIVE;
import static de.ovgu.featureide.fm.core.localization.StringTable.IN_CLASS;
import static de.ovgu.featureide.fm.core.localization.StringTable.MAXIMUM_FEATURES_PER_DIRECTIVE;
import static de.ovgu.featureide.fm.core.localization.StringTable.MAXIMUM_NESTING_OF_DIRECTIVES;
import static de.ovgu.featureide.fm.core.localization.StringTable.MINIMUM_FEATURES_PER_DIRECTIVE;
import static de.ovgu.featureide.fm.core.localization.StringTable.NUMBER_OF_DIRECTIVES;
import static de.ovgu.featureide.fm.core.localization.StringTable.PROJECT_STATISTICS;

import java.util.List;

import de.ovgu.featureide.core.fstmodel.FSTClass;
import de.ovgu.featureide.core.fstmodel.FSTModel;
import de.ovgu.featureide.ui.statistics.core.composite.LazyParent;
import de.ovgu.featureide.ui.statistics.core.composite.Parent;
import de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations.genericdatatypes.AbstractSortModeNode;

/**
 * TreeNode who stores the number of used preprocessor directives, directives
 * per class and features per directives.<br>
 * This node should only be used for a preprocessor project.
 * 
 * @author Dominik Hamann
 * @author Patrick Haese
 */
public class DirectivesNode extends LazyParent {
	
	private final FSTModel fstModel;

	/**
	 * Constructor for a {@code DirectivesNode}.
	 * 
	 * @param description
	 *            description of the node shown in the view
	 * @param fstModel
	 *            FSTModel for the calculation
	 */
	public DirectivesNode(String description, FSTModel fstModel) {
		super(description);
		this.fstModel = fstModel;
	}

	@Override
	protected void initChildren() {
		final Parent internClasses = new Parent("Classes");
		Parent project = new Parent(PROJECT_STATISTICS);
		Integer maxNesting = 0;
		String maxNestingClass = null;
		
		final Aggregator aggProject = new Aggregator();
		aggProject.processAll(fstModel);
		
		Parent directives = new Parent(NUMBER_OF_DIRECTIVES);
		directives.setValue(aggProject.getDirectiveCount());
		project.addChild(directives);
		
//		project.addChild(new LazyParent(NUMBER_OF_DIRECTIVES) {
//			@Override
//			protected void initChildren() {
//				new Aggregator().processAll(fstModel);
//			}
//		});

		
		for (FSTClass clazz : fstModel.getClasses()) {
			String className = clazz.getName();
			final int pIndex = className.lastIndexOf('/');
			className = ((pIndex > 0) ? className.substring(0, pIndex + 1).replace('/', '.') : "(default package).") + className.substring(pIndex + 1);

			final Parent classNode = new Parent(className);
			classNode.setValue(aggProject.getDirectiveCountForClass(className));
			internClasses.addChild(classNode);

			if (!clazz.getRoles().isEmpty()) {
				final Integer currentNesting = aggProject.getMaxNesting();
				classNode.addChild(new Parent(MAXIMUM_NESTING_OF_DIRECTIVES, currentNesting));
				if (currentNesting > maxNesting) {
					maxNesting = currentNesting;
					maxNestingClass = className;
				}
				aggProject.setMaxNesting(0);
			}
		}

		final Integer maximumSum = aggProject.getMaximumSum();
		final Integer minimumSum = aggProject.getMinimumSum();

		final Parent directivesPerClass = new Parent(DIRECTIVES_PER_CLASS);
		directivesPerClass.addChild(new Parent("Maximum number of directives: " + maximumSum + IN_CLASS
				+ searchClass(internClasses.getChildren(), maximumSum)));
		directivesPerClass.addChild(new Parent("Minimum number of directives: " + minimumSum + IN_CLASS
				+ searchClass(internClasses.getChildren(), minimumSum)));
		directivesPerClass.addChild(new Parent(AVERAGE_NUMBER_OF_DIRECTIVES_PER_CLASS, getAverage(internClasses)));
		project.addChild(directivesPerClass);

		project.addChild(new LazyParent(FEATURES_PER_DIRECTIVE) {

			@Override
			protected void initChildren() {

				Aggregator aggregator = new Aggregator();

				//aggregator.initializeDirectiveCount(fstModel);

				List<Integer> list = aggregator.getListOfNestings();
				double average = 0.0;
				for (Integer i : list) {
					average += i;
				}
				if (list.size() != 0) {
					average /= list.size();
					average *= 10;
					long rounded = Math.round(average);
					average = ((double) rounded) / 10;
				} else {
					average = 0.0;
				}

				addChild(new Parent(MAXIMUM_FEATURES_PER_DIRECTIVE, aggregator.getMaxNesting()));
				addChild(new Parent(MINIMUM_FEATURES_PER_DIRECTIVE, aggregator.getMinNesting()));
				addChild(new Parent(AVERAGE_FEATURES_PER_DIRECTIVE, average));
			}
		});
		project.addChild(new Parent("Maximum nesting of directives: " + maxNesting + IN_CLASS + maxNestingClass));

		addChild(project);

		Parent classes = new AbstractSortModeNode(CLASS_STATISTICS) {
			@Override
			protected void initChildren() {
				for (Parent child : internClasses.getChildren()) {
					addChild(child);
				}
			}
		};

		addChild(classes);
	}

	private String searchClass(Parent[] data, Integer input) {
		for (Parent p : data) {
			if (p.getValue().equals(input)) {
				String className = p.getDescription();
				return className;
			}
		}
		return null;
	}

	private Double getAverage(Parent parent) {
		if (parent.hasChildren()) {
			Integer numberOfDirectives = 0;
			for (Parent child : parent.getChildren()) {
				numberOfDirectives += (Integer) child.getValue();
			}

			Integer numberOfChildren = parent.getChildren().length;

			double average = numberOfDirectives;

			average /= (double) numberOfChildren;
			average *= 10;
			long rounded = Math.round(average);
			average = ((double) rounded) / 10;

			return average;
		}

		return 0.0;
	}

}
