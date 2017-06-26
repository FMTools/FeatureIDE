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

import java.util.Arrays;
import java.util.Collection;

/**
 * Represents an instance of a satisfiability problem in CNF.<br/>
 * Use a {@link ISatSolverProvider solver provider} or the {@link #getSolver()}
 * method to get a {@link BasicSolver solver} for this problem.
 * 
 * @author Sebastian Krieter
 */
public class SlicedVariables extends Variables {

	private static final long serialVersionUID = 2162112716200473939L;

	private final int[] orgToInternal;
	private final int[] internalToOrg;

	public SlicedVariables(Variables orgVariables, Collection<String> varNameList) {
		super(orgVariables);

		orgToInternal = new int[orgVariables.maxVariableID() + 1];
		internalToOrg = new int[varNameList.size() + 1];

		for (String varName : varNameList) {
			final int orgVariable = orgVariables.getVariable(varName);
			orgToInternal[orgVariable] = 1;
		}

		int count = 0;
		for (int i = 1; i < orgToInternal.length; i++) {
			final int index = orgToInternal[i];
			if (index > 0) {
				count++;
				orgToInternal[i] = count;
				internalToOrg[count] = i;
			}
		}
	}

	private SlicedVariables(SlicedVariables oldSatMapping) {
		super(oldSatMapping);
		this.orgToInternal = Arrays.copyOf(oldSatMapping.orgToInternal, oldSatMapping.orgToInternal.length);
		this.internalToOrg = Arrays.copyOf(oldSatMapping.internalToOrg, oldSatMapping.internalToOrg.length);
	}

	@Override
	public int size() {
		return internalToOrg.length - 1;
	}

	@Override
	public SlicedVariables clone() {
		return new SlicedVariables(this);
	}

	public boolean checkClause(LiteralSet orgClause) {
		for (int literal : orgClause.getLiterals()) {
			if (orgToInternal[Math.abs(literal)] == 0) {
				return false;
			}
		}
		return true;
	}

	public LiteralSet convertToInternal(LiteralSet orgClause) {
		return new LiteralSet(convertToInternal(orgClause.getLiterals()));
	}

	public int[] convertToInternal(int[] orgLiterals) {
		final int[] convertedLiterals = new int[orgLiterals.length];
		for (int i = 0; i < orgLiterals.length; i++) {
			convertedLiterals[i] = convertToInternal(orgLiterals[i]);
		}
		return convertedLiterals;
	}

	public int convertToInternal(int orgLiteral) {
		final int convertedLiteral = orgToInternal[Math.abs(orgLiteral)];
		assert convertedLiteral != 0;
		return orgLiteral > 0 ? convertedLiteral : -convertedLiteral;
	}

	public LiteralSet convertToOriginal(LiteralSet internalClause) {
		return new LiteralSet(convertToInternal(internalClause.getLiterals()));
	}

	public int[] convertToOriginal(int[] internalLiterals) {
		final int[] convertedLiterals = new int[internalLiterals.length];
		for (int i = 0; i < internalLiterals.length; i++) {
			convertedLiterals[i] = convertToOriginal(internalLiterals[i]);
		}
		return convertedLiterals;
	}

	public int convertToOriginal(int internalLiteral) {
		final int convertedLiteral = internalToOrg[Math.abs(internalLiteral)];
		return internalLiteral > 0 ? convertedLiteral : -convertedLiteral;
	}

}
