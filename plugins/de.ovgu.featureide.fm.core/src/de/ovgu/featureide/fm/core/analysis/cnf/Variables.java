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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an instance of a satisfiability problem in CNF.<br/>
 * Use a {@link ISatSolverProvider solver provider} or the {@link #getSolver()}
 * method to get a {@link BasicSolver solver} for this problem.
 * 
 * @author Sebastian Krieter
 */
public class Variables implements Serializable, IVariables, IInternalVariables {

	private static final long serialVersionUID = -1767212780361483105L;

	protected final String[] intToVar;
	protected final Map<String, Integer> varToInt;

	public Variables() {
		this.intToVar = new String[0];
		this.varToInt = Collections.emptyMap();
	}

	public Variables(Collection<String> varNameList) {
		this.intToVar = new String[varNameList.size() + 1];
		this.varToInt = new HashMap<>((int) (1.5 * varNameList.size()));

		int index = 0;
		for (String feature : varNameList) {
			final String name = feature.toString();
			if (name == null) {
				throw new RuntimeException();
			}
			varToInt.put(name, ++index);
			intToVar[index] = name;
		}
	}

	protected Variables(Variables oldSatMapping) {
		this.intToVar = Arrays.copyOf(oldSatMapping.intToVar, oldSatMapping.intToVar.length);
		this.varToInt = new HashMap<>(oldSatMapping.varToInt);
	}

	@Override
	public List<String> convertToString(LiteralSet model) {
		return convertToString(model, true, false);
	}

	@Override
	public List<String> convertToString(LiteralSet model, boolean includePositive, boolean includeNegative) {
		return convertToString(model, includePositive, includeNegative, true);
	}

	@Override
	public List<String> convertToString(LiteralSet model, boolean includePositive, boolean includeNegative, boolean markNegative) {
		final List<String> resultList = new ArrayList<>();
		for (int var : model.getLiterals()) {
			if (var > 0) {
				if (includePositive) {
					resultList.add(intToVar[Math.abs(var)]);
				}
			} else {
				if (includeNegative) {
					if (markNegative) {
						resultList.add("-" + intToVar[Math.abs(var)]);
					} else {
						resultList.add(intToVar[Math.abs(var)]);
					}
				}
			}
		}
		return resultList;
	}

	@Override
	public LiteralSet convertToVariables(List<String> variableNames) {
		final int[] literals = new int[variableNames.size()];
		int i = 0;
		for (String varName : variableNames) {
			literals[i++] = varToInt.get(varName);
		}
		return new LiteralSet(literals);
	}

	@Override
	public LiteralSet convertToVariables(List<String> variableNames, boolean sign) {
		final int[] literals = new int[variableNames.size()];
		int i = 0;
		for (String varName : variableNames) {
			literals[i++] = sign ? varToInt.get(varName) : -varToInt.get(varName);
		}
		return new LiteralSet(literals);
	}

	@Override
	public int size() {
		return intToVar.length - 1;
	}

	@Override
	public int maxVariableID() {
		return intToVar.length - 1;
	}

	@Override
	public int getVariable(String varName) {
		final Integer var = varToInt.get(varName);
		return var == null ? 0 : var;
	}

	@Override
	public int getVariable(String varName, boolean sign) {
		final Integer variable = varToInt.get(varName);
		return variable == null ? 0 : sign ? variable : -variable;
	}

	@Override
	public String getName(final int x) {
		return intToVar[Math.abs(x)];
	}

	@Override
	public String[] getNames() {
		return intToVar;
	}

	@Override
	public Variables clone() {
		return new Variables(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(intToVar);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return Arrays.equals(intToVar, ((Variables) obj).intToVar);
	}

	public boolean checkClause(LiteralSet orgClause) {
		return true;
	}

	public LiteralSet convertToInternal(LiteralSet orgClause) {
		return orgClause;
	}

	public int[] convertToInternal(int[] orgLiterals) {
		return orgLiterals;
	}

	public int convertToInternal(int orgLiteral) {
		return orgLiteral;
	}

	public LiteralSet convertToOriginal(LiteralSet internalClause) {
		return internalClause;
	}

	public int[] convertToOriginal(int[] internalLiterals) {
		return internalLiterals;
	}

	public int convertToOriginal(int internalLiteral) {
		return internalLiteral;
	}

}
