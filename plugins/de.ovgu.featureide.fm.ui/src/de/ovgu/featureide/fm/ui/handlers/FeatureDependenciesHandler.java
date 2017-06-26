/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.ui.handlers;

import static de.ovgu.featureide.fm.core.localization.StringTable.CALCULATING_FEATURE_DEPENDENCIES;

import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.base.IModalImplicationGraph;
import de.ovgu.featureide.fm.core.io.FileSystem;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.ui.handlers.base.AFileHandler;

/**
 * calculates and shows dependencies between features in a MessageBox
 * 
 * @author Fabian Benduhn
 * @author Marcus Pinnecke
 */
public class FeatureDependenciesHandler extends AFileHandler {

	@Override
	protected void singleAction(final IFile inputFile) {
		// Ask for file name
		FileDialog fileDialog = new FileDialog(new Shell(), SWT.SAVE);
		fileDialog.setFileName("*.txt");
		fileDialog.setOverwrite(true);
		final String outputFile = fileDialog.open();
		if (outputFile == null) {
			return;
		}

		final FeatureModelManager instance = FeatureModelManager.getInstance(Paths.get(inputFile.getLocationURI()));
		Job job = new Job(CALCULATING_FEATURE_DEPENDENCIES) {
			protected IStatus run(IProgressMonitor monitor) {
				final IModalImplicationGraph modalImplicationGraph = instance.getSnapshot().getFormula().getModalImplicationGraph();
				// TODO !!!
				final String text = modalImplicationGraph.toString();
				try {
					FileSystem.write(Paths.get(outputFile), text);
				} catch (IOException e) {
					Logger.logError(e);
				}
				return Status.OK_STATUS;
			}

		};
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

}
