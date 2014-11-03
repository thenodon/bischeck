/*
#
# Copyright (C) 2010-2014 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
 */
package com.ingby.socbox.bischeck.notifications;

public class NotifierException extends Exception {

    private static final long serialVersionUID = 941565193879100650L;

    public NotifierException() {
        super();
    }

    public NotifierException(String message) {
        super(message);
    }

    public NotifierException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotifierException(Throwable cause) {
        super(cause);
    }
}
