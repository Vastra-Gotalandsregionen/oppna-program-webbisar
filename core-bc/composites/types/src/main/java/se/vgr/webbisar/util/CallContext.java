/**
 * Copyright 2009 Vastra Gotalandsregionen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public
 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *   Boston, MA 02111-1307  USA
 */
package se.vgr.webbisar.util;

import java.io.Serializable;

public class CallContext implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String userId;
	
	public CallContext(String userId) {
		this.userId = userId;
	}
	
	public String getUserId() {
		return userId;
	}

	@Override
	public String toString() {
		return new StringBuffer().append("usedId=").append(userId).toString();
	}
}
