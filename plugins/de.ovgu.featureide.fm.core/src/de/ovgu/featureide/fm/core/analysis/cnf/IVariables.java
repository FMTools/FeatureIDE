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
package de.ovgu.featureide.fm.core.analysis.cnf;

import java.util.List;

/**
 * Represents an instance of a satisfiability problem in CNF.</br>
 * Use a {@link ISatSolverProvider solver provider} or the {@link #getSolver()}
 * method to get a {@link BasicSolver solver} for this problem.
 * 
 * @author Sebastian Krieter
 */
public interface IVariables extends Cloneable {

	List<String> convertToString(LiteralSet model);

	List<String> convertToString(LiteralSet model, boolean includePositive, boolean includeNegative);

	List<String> convertToString(LiteralSet model, boolean includePositive, boolean includeNegative, boolean markNegative);

	LiteralSet convertToVariables(List<String> variableNames);

	LiteralSet convertToVariables(List<String> variableNames, boolean sign);

	int size();

	int maxVariableID();

	int getVariable(String varName);

	int getVariable(String varName, boolean sign);

	String getName(final int x);

	String[] getNames();

	IVariables clone();

}
