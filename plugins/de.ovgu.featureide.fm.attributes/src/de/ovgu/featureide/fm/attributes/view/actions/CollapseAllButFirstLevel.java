package de.ovgu.featureide.fm.attributes.view.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;

public class CollapseAllButFirstLevel extends Action {

	private TreeViewer view;

	public CollapseAllButFirstLevel(TreeViewer view, ImageDescriptor icon) {
		super("", icon);
		this.view = view;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		view.collapseAll();
		view.expandToLevel(2);
	}
}
