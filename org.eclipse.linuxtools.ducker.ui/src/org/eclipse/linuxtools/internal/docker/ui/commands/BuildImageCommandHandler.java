/*******************************************************************************
 * Copyright (c) 2015 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.linuxtools.internal.docker.ui.commands;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.ui.views.DVMessages;
import org.eclipse.linuxtools.internal.docker.ui.views.DockerImagesView;
import org.eclipse.linuxtools.internal.docker.ui.views.ImageBuildProgressHandler;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageBuild;
import org.eclipse.linuxtools.internal.docker.ui.wizards.WizardMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class BuildImageCommandHandler extends AbstractHandler {

	private final static String BUILD_IMAGE_JOB_TITLE = "ImageBuild.msg"; //$NON-NLS-1$
	private static final String ERROR_BUILDING_IMAGE = "ImageBuildError.msg"; //$NON-NLS-1$
	private static final String IMAGE_DIRECTORY_VALIDATE = "ImageDirectoryValidate.msg"; //$NON-NLS-1$
	
	private IDockerConnection connection;

	@Override
	public Object execute(final ExecutionEvent event) {
		final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		final ImageBuild wizard = new ImageBuild();
		final boolean buildImage = CommandUtils.openWizard(wizard,
				HandlerUtil.getActiveShell(event));
		if (buildImage) {
			if (activePart instanceof DockerImagesView) {
				connection = ((DockerImagesView) activePart)
						.getConnection();
			}
			performBuildImage(wizard);
		}
		return null;
	}
	
	private void performBuildImage(final ImageBuild wizard) {
		final Job buildImageJob = new Job(
				DVMessages.getString(BUILD_IMAGE_JOB_TITLE)) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				final String id = wizard.getImageName();
				final int lines = wizard.getNumberOfLines();
				final IPath path = wizard.getDirectory();

				monitor.beginTask(DVMessages.getString(BUILD_IMAGE_JOB_TITLE),
						2);
				monitor.subTask(
						WizardMessages.getString(IMAGE_DIRECTORY_VALIDATE));
				try {
					Files.walkFileTree(Paths.get(path.toString()),
							new FileVisitor<java.nio.file.Path>() {
								@Override
								public FileVisitResult preVisitDirectory(
										java.nio.file.Path dir,
										BasicFileAttributes attrs) {
									return FileVisitResult.CONTINUE;
								}
								@Override
								public FileVisitResult visitFile(
										java.nio.file.Path file,
										BasicFileAttributes attrs) throws IOException {
									if (!file.toFile().canRead()) {
										throw new IOException();
									}
									return FileVisitResult.CONTINUE;
								}
								@Override
								public FileVisitResult visitFileFailed(
										java.nio.file.Path file, IOException exc)
												throws IOException {
									throw exc;
								}
								@Override
								public FileVisitResult postVisitDirectory(
										java.nio.file.Path dir, IOException exc) {
									return FileVisitResult.CONTINUE;
								}
							});
				} catch (final IOException e) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(
									Display.getCurrent().getActiveShell(),
											WizardMessages.getString(
													"ErrorInvalidDirectory.msg"),
									WizardMessages.getFormattedString(
											"ErrorInvalidPermissions.msg",
											path.toString()));
						}
					});
					return Status.OK_STATUS;
				}
				monitor.worked(1);

				// build the image and let the progress
				// handler refresh the images when done
				try {
					monitor.subTask(
							DVMessages.getString(BUILD_IMAGE_JOB_TITLE));
					((DockerConnection) connection)
							.buildImage(path, id,
									new ImageBuildProgressHandler(connection,
											id, lines));
					monitor.worked(1);
				} catch (final DockerException e) {
					Display.getDefault().syncExec(new Runnable() {

						@Override
						public void run() {
							MessageDialog.openError(Display.getCurrent()
									.getActiveShell(), DVMessages
									.getFormattedString(ERROR_BUILDING_IMAGE,
											id), e.getMessage());

						}

					});
					// for now
				} catch (InterruptedException e) {
					// do nothing
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

		};

		buildImageJob.schedule();

	}

}
