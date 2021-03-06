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

package org.eclipse.linuxtools.docker.core;

/**
 * A tag (or version) for a given repository.
 */
public interface IRepositoryTag {

	/**
	 * @return Name of the tag.
	 */
	String getName();

	/**
	 * @return The corresponding image layer for this specific tag
	 */
	String getLayer();
}
