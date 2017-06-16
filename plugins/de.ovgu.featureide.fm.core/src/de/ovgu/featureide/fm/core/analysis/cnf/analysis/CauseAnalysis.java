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
package de.ovgu.featureide.fm.core.analysis.cnf.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.ISatSolver;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Finds clauses responsible for core and dead features.
 * 
 * @author Sebastian Krieter
 */
public class CauseAnalysis extends AClauseAnalysis<List<CauseAnalysis.Anomalies>> {

	public static class Anomalies {

		protected LiteralSet deadVariables = new LiteralSet();
		protected List<LiteralSet> redundantClauses = Collections.emptyList();

		public LiteralSet getDeadVariables() {
			return deadVariables;
		}

		public void setDeadVariables(LiteralSet variables) {
			if (variables == null) {
				this.deadVariables = new LiteralSet();
			} else {
				this.deadVariables = variables;
			}
		}

		public List<LiteralSet> getRedundantClauses() {
			return redundantClauses;
		}

		public void setRedundantClauses(List<LiteralSet> redundantClauses) {
			if (redundantClauses == null) {
				this.redundantClauses = Collections.emptyList();
			} else {
				this.redundantClauses = redundantClauses;
			}
		}

	}

	public CauseAnalysis(CNF satInstance) {
		super(satInstance);
	}

	public CauseAnalysis(ISatSolver solver) {
		super(solver);
	}

	private Anomalies anomalies;

	public Anomalies getAnomalies() {
		return anomalies;
	}

	public void setAnomalies(Anomalies anomalies) {
		this.anomalies = anomalies;
	}

	public List<Anomalies> analyze(IMonitor monitor) throws Exception {
		if (clauseList == null) {
			return Collections.emptyList();
		}
		if (clauseGroupSize == null) {
			clauseGroupSize = new int[clauseList.size()];
			Arrays.fill(clauseGroupSize, 1);
		}
		final List<Anomalies> resultList = new ArrayList<>(clauseGroupSize.length);
		for (int i = 0; i < clauseList.size(); i++) {
			resultList.add(null);
		}
		if (anomalies == null) {
			return resultList;
		}
		monitor.setRemainingWork(clauseList.size() + 3);

		LiteralSet remainingVariables = anomalies.deadVariables.getVariables();
		List<LiteralSet> remainingClauses = new ArrayList<>(anomalies.redundantClauses);
		monitor.step();

		if (!remainingClauses.isEmpty()) {
			List<LiteralSet> newClauseList = Functional.removeNull(LongRunningWrapper.runMethod(new IndependentRedundancyAnalysis(solver, remainingClauses)));
			remainingClauses.removeAll(newClauseList);
		}
		monitor.step();

		if (remainingVariables.getLiterals().length > 0) {
			remainingVariables = remainingVariables.removeAll(LongRunningWrapper.runMethod(new CoreDeadAnalysis(solver, remainingVariables)));
		}
		monitor.step();

		int endIndex = 0;
		for (int i = 0; i < clauseGroupSize.length; i++) {
			if (remainingVariables.getLiterals().length == 0 && remainingClauses.isEmpty()) {
				break;
			}

			final int startIndex = endIndex;
			endIndex += clauseGroupSize[i];
			solver.addClauses(clauseList.subList(startIndex, endIndex));

			if (remainingVariables.getLiterals().length > 0) {
				final LiteralSet deadVariables = LongRunningWrapper.runMethod(new CoreDeadAnalysis(solver, remainingVariables));
				if (deadVariables.getLiterals().length != 0) {
					getAnomalies(resultList, i).setDeadVariables(deadVariables);
					remainingVariables = remainingVariables.removeAll(deadVariables);
				}
			}

			if (!remainingClauses.isEmpty()) {
				List<LiteralSet> newClauseList = Functional
						.removeNull(LongRunningWrapper.runMethod(new IndependentRedundancyAnalysis(solver, remainingClauses)));
				if (!newClauseList.isEmpty()) {
					getAnomalies(resultList, i).setRedundantClauses(newClauseList);
					remainingClauses.removeAll(newClauseList);
				}
			}

			monitor.step();
		}

		return resultList;
	}

	protected Anomalies getAnomalies(final List<Anomalies> resultList, final Integer curIndex) {
		Anomalies curAnomalies = resultList.get(curIndex);
		if (curAnomalies == null) {
			curAnomalies = new Anomalies();
			resultList.set(curIndex, curAnomalies);
		}
		return curAnomalies;
	}

}
