/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.test.tool.rpc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.impl.NetconfServerSession;
import org.opendaylight.netconf.impl.mapping.operations.DefaultNetconfOperation;
import org.opendaylight.netconf.util.mapping.AbstractLastNetconfOperation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SimulatedCreateSubscription extends AbstractLastNetconfOperation implements DefaultNetconfOperation {

    private final Map<Notification, NetconfMessage> notifications;
    private NetconfServerSession session;
    private ScheduledExecutorService scheduledExecutorService;

    public SimulatedCreateSubscription(final String id, final Optional<File> notificationsFile) {
        super(id);

        final Optional<Notifications> notifs;

        if (notificationsFile.isPresent()) {
            notifs = Optional.of(loadNotifications(notificationsFile.get()));
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
        } else {
            notifs = Optional.absent();
        }

        if (notifs.isPresent()) {
            final Collection<Notification> toCopy = notifs.get().getNotificationList();
            final Map<Notification, NetconfMessage> preparedMessages = Maps.newHashMapWithExpectedSize(toCopy.size());
            for (final Notification notification : toCopy) {
                final NetconfMessage parsedNotification = parseNetconfNotification(notification.getContent());
                preparedMessages.put(notification, parsedNotification);
            }
            this.notifications = preparedMessages;
        } else {
            this.notifications = Collections.emptyMap();
        }
    }

    private static Notifications loadNotifications(final File file) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Notifications.class);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (Notifications) jaxbUnmarshaller.unmarshal(file);
        } catch (final JAXBException e) {
            throw new IllegalArgumentException("Canot parse file " + file + " as a notifications file", e);
        }
    }

    @Override
    protected String getOperationName() {
        return "create-subscription";
    }

    @Override
    protected String getOperationNamespace() {
        return "urn:ietf:params:xml:ns:netconf:notification:1.0";
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) {
        long delayAggregator = 0;

        for (final Map.Entry<Notification, NetconfMessage> notification : notifications.entrySet()) {
            for (int i = 0; i <= notification.getKey().getTimes(); i++) {

                delayAggregator += notification.getKey().getDelayInSeconds();

                scheduledExecutorService.schedule(() -> {
                    Preconditions.checkState(session != null, "Session is not set, cannot process notifications");
                    session.sendMessage(notification.getValue());
                }, delayAggregator, TimeUnit.SECONDS);
            }
        }
        return document.createElement(XmlNetconfConstants.OK);
    }

    private static NetconfMessage parseNetconfNotification(String content) {
        final int startEventTime = content.indexOf("<eventTime>") + "<eventTime>".length();
        final int endEventTime = content.indexOf("</eventTime>");
        final String eventTime = content.substring(startEventTime, endEventTime);
        if (eventTime.equals("XXXX")) {
            content = content.replace(eventTime, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        }

        try {
            return new NetconfMessage(XmlUtil.readXmlToDocument(content));
        } catch (SAXException | IOException e) {
            throw new IllegalArgumentException("Cannot parse notifications", e);
        }
    }

    @Override
    public void setNetconfSession(final NetconfServerSession newSession) {
        this.session = newSession;
    }

    @XmlRootElement(name = "notifications")
    public static final class Notifications {

        @javax.xml.bind.annotation.XmlElement(nillable =  false, name = "notification", required = true)
        private List<Notification> notificationList;

        public List<Notification> getNotificationList() {
            return notificationList;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Notifications{");
            sb.append("notificationList=").append(notificationList);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final class Notification {

        @javax.xml.bind.annotation.XmlElement(nillable = false, name = "delay")
        private long delayInSeconds;

        @javax.xml.bind.annotation.XmlElement(nillable = false, name = "times")
        private long times;

        @javax.xml.bind.annotation.XmlElement(nillable = false, name = "content", required = true)
        private String content;

        public long getDelayInSeconds() {
            return delayInSeconds;
        }

        public long getTimes() {
            return times;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Notification{");
            sb.append("delayInSeconds=").append(delayInSeconds);
            sb.append(", times=").append(times);
            sb.append(", content='").append(content).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
