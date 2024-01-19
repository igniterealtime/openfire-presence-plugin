/*
 * Copyright (C) 2004-2008 Jive Software, 2024 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.plugin.presence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.plugin.PresencePlugin;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Presence;

/**
 * Servlet that provides information about the presence status of the users in the system.
 * The information may be provided in XML format or in graphical mode. Use the <b>type</b>
 * parameter to specify the type of information to get. Possible values are <b>image</b> and
 * <b>xml</b>. If no type was defined then an image representation is assumed.<p>
 * <p/>
 * The request <b>MUST</b> include the <b>jid</b> parameter. This parameter will be used
 * to locate the local user in the server. If this parameter is missing from the request then
 * an error will be logged and nothing will be returned.
 *
 * @author Gaston Dombiak
 */
public class PresenceStatusServlet extends HttpServlet {

    private static final Logger Log = LoggerFactory.getLogger(PresenceStatusServlet.class);
    
    private PresencePlugin plugin;
    private XMLPresenceProvider xmlProvider;
    private ImagePresenceProvider imageProvider;
    private TextPresenceProvider textProvider;

    private byte[] available;
    private byte[] away;
    private byte[] chat;
    private byte[] dnd;
    private byte[] offline;
    private byte[] xa;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        plugin =
                (PresencePlugin) XMPPServer.getInstance().getPluginManager().getPlugin("presence");
        xmlProvider = new XMLPresenceProvider();
        imageProvider = new ImagePresenceProvider(this);
        textProvider = new TextPresenceProvider();
        // Exclude this servlet from requiring the user to login
        AuthCheckFilter.addExclude("presence/status");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sender = request.getParameter("req_jid");
        String jid = request.getParameter("jid");
        String type = request.getParameter("type");
        type = type == null ? "image" : type;

        try {
            Presence presence = plugin.getPresence(sender, jid);
            switch (type) {
                case "image":
                    imageProvider.sendInfo(request, response, presence);
                    break;
                case "xml":
                    xmlProvider.sendInfo(request, response, presence);
                    break;
                case "text":
                    textProvider.sendInfo(request, response, presence);
                    break;
                default:
                    Log.warn("The presence servlet received an invalid request of type: " + type);
                    // TODO Do something
                    break;
            }
        }
        catch (UserNotFoundException | IllegalArgumentException e) {
            switch (type) {
                case "image":
                    imageProvider.sendUserNotFound(request, response);
                    break;
                case "xml":
                    xmlProvider.sendUserNotFound(request, response);
                    break;
                case "text":
                    textProvider.sendUserNotFound(request, response);
                    break;
                default:
                    Log.warn("The presence servlet received an invalid request of type: " + type);
                    // TODO Do something
                    break;
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        available = null;
        away = null;
        chat = null;
        dnd = null;
        offline = null;
        xa = null;
        // Release the excluded URL
        AuthCheckFilter.removeExclude("presence/status");
    }

    private byte[] loadResource(String path) {
        ServletContext context = getServletContext();
        try (InputStream in = context.getResourceAsStream(path);
            ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            for (int i = in.read(); i > -1; i = in.read()) {
                out.write(i);
            }
            return out.toByteArray();
        }
        catch (Throwable e) {
            Log.error("error loading: {}", path, e);
        }
        return null;
    }

    public synchronized byte[] getAvailable()
    {
        if (available == null) {
            available = loadResource("/images/user-green-16x16.gif");
        }
        return available;
    }

    public synchronized byte[] getAway()
    {
        if (away == null) {
            away = loadResource("/images/user-yellow-16x16.gif");
        }
        return away;
    }

    public synchronized byte[] getChat()
    {
        if (chat == null) {
            chat = loadResource("/images/user-green-16x16.gif");
        }
        return chat;
    }

    public synchronized byte[] getDnd()
    {
        if (dnd == null) {
            dnd = loadResource("/images/user-red-16x16.gif");
        }
        return dnd;
    }

    public synchronized byte[] getOffline()
    {
        if (offline == null) {
            offline = loadResource("/images/user-clear-16x16.gif");
        }
        return offline;
    }

    public byte[] getXa()
    {
        if (xa == null) {
            xa = loadResource("/images/user-yellow-16x16.gif");
        }
        return xa;
    }
}
