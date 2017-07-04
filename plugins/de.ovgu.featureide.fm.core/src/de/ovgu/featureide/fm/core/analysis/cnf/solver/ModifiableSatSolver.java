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
package de.ovgu.featureide.fm.core.analysis.cnf.solver;

import java.util.ArrayList;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.IConstr;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;

/**
 * {@link AdvancedSatSolver} version that supports removing clauses.
 * 
 * @author Sebastian Krieter
 */
public class ModifiableSatSolver extends AdvancedSatSolver {

	public ModifiableSatSolver(AdvancedSatSolver oldSolver) throws RuntimeContradictionException {
		super(oldSolver);
	}

	public ModifiableSatSolver(CNF satInstance) throws RuntimeContradictionException {
		super(satInstance);
	}

	@Override
	public List<IConstr> addClauses(Iterable<? extends LiteralSet> clauses) throws RuntimeContradictionException {
		final ArrayList<IConstr> newConstrs = new ArrayList<>();

		try {
			for (LiteralSet clause : clauses) {
				newConstrs.add(addClauseInternal(solver, clause.getLiterals(), 0 , clause.size()));
			}
		} catch (RuntimeContradictionException e) {
			removeLastClauses(newConstrs.size());
			throw e;
		}

		return newConstrs;
	}

	@Override
	protected IConstr addClauseInternal(Solver<?> solver, int[] mainClause, int start, int end) throws RuntimeContradictionException {
		final IConstr constr = super.addClauseInternal(solver, mainClause, start, end);
		constrList.add(constr);
		return constr;
	}

	protected IConstr addClauseInternal(Solver<?> solver, VecInt vec) throws RuntimeContradictionException {
		final IConstr constr = super.addClauseInternal(solver, vec);
		constrList.add(constr);
		return constr;
	}

	@Override
	public void removeClause(IConstr constr) {
		if (constr != null) {
			try {
				solver.removeConstr(constr);
			} catch (Exception e) {
				throw new RuntimeContradictionException(e);
			}
		}
	}

	public void removeLastClauses(int numberOfClauses) {
		try {
			for (int i = 0; i < numberOfClauses; i++) {
				final IConstr removeLast = constrList.remove(constrList.size() - 1);
				if (removeLast != null) {
					solver.removeSubsumedConstr(removeLast);
				}
			}
			solver.clearLearntClauses();
		} catch (Exception e) {
			throw new RuntimeContradictionException(e);
		}
	}

	protected void configureSolver(Solver<?> solver) {
		solver.setTimeoutMs(1000);
		solver.setDBSimplificationAllowed(false);
		solver.setVerbose(false);
	}

	@Override
	public ModifiableSatSolver clone() {
		if (this.getClass() == ModifiableSatSolver.class) {
			return new ModifiableSatSolver(this);
		} else {
			throw new RuntimeException("Cloning not supported for " + this.getClass().toString());
		}
	}

}