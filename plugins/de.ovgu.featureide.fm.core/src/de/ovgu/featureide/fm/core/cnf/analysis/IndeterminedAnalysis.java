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
package de.ovgu.featureide.fm.core.cnf.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sat4j.core.VecInt;

import de.ovgu.featureide.fm.core.cnf.CNF;
import de.ovgu.featureide.fm.core.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.cnf.SatUtils;
import de.ovgu.featureide.fm.core.cnf.manipulator.remove.CNFSlicer;
import de.ovgu.featureide.fm.core.cnf.solver.ISatSolver2;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.cnf.solver.ModifiableSatSolver;
import de.ovgu.featureide.fm.core.cnf.solver.RuntimeContradictionException;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Finds core and dead features.
 * 
 * @author Sebastian Krieter
 */
public class IndeterminedAnalysis extends AVariableAnalysis<LiteralSet> {

	public IndeterminedAnalysis(CNF satInstance) {
		super(satInstance);
	}

	public IndeterminedAnalysis(ISatSolver2 solver) {
		super(solver);
	}

	public LiteralSet analyze(IMonitor monitor) throws Exception {
		monitor.setRemainingWork(variables.getLiterals().length + 1);

		final VecInt resultList = new VecInt();
		final List<LiteralSet> relevantClauses = new ArrayList<>();

		for (int literal : variables.getLiterals()) {
			final CNF slicedCNF = LongRunningWrapper.runMethod(new CNFSlicer(solver.getSatInstance(), variables.removeAll(new LiteralSet(literal))));
			final List<LiteralSet> clauses = slicedCNF.getClauses();
			final ModifiableSatSolver modSolver = new ModifiableSatSolver(slicedCNF);
			for (LiteralSet clause : clauses) {
				if (clause.contains(literal)) {
					final LiteralSet newClause = LiteralSet.cleanLiteralSet(clause, literal);
					if (newClause != null) {
						relevantClauses.add(newClause);
					}
				}
			}
			try {
				modSolver.addClauses(relevantClauses);
			} catch (RuntimeContradictionException e) {
				relevantClauses.clear();
				monitor.step();
				continue;
			}

			final SatResult hasSolution = modSolver.hasSolution();
			switch (hasSolution) {
			case FALSE:
			case TIMEOUT:
				break;
			case TRUE:
				resultList.push(literal);
				break;
			default:
				throw new AssertionError(hasSolution);
			}
			modSolver.removeLastClauses(relevantClauses.size());

			relevantClauses.clear();
			monitor.step();
		}

		return new LiteralSet(Arrays.copyOf(resultList.toArray(), resultList.size()));
	}

	protected final boolean isRedundant(ISimpleSatSolver solver, LiteralSet curClause) {
		final SatResult hasSolution = solver.hasSolution(SatUtils.negateSolution(curClause.getLiterals()));
		switch (hasSolution) {
		case FALSE:
			return true;
		case TIMEOUT:
		case TRUE:
			return false;
		default:
			throw new AssertionError(hasSolution);
		}
	}

}