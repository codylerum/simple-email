/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.outjected.email.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.outjected.email.api.ContentDisposition;
import com.outjected.email.api.ContentType;
import com.outjected.email.api.Header;
import com.outjected.email.api.InvalidAddressException;
import com.outjected.email.api.MailHeader;
import com.outjected.email.api.MessagePriority;
import com.outjected.email.api.RecipientType;
import com.outjected.email.impl.attachments.AttachmentPart;
import com.outjected.email.impl.attachments.BaseAttachment;
import com.outjected.email.impl.util.MailUtility;
import com.sun.mail.smtp.SMTPMessage;

/**
 * @author Cody Lerum
 */
public class BaseMailMessage {
    private RootMimeMessage rootMimeMessage;
    private String charset;
    private ContentType rootContentType;
    private Map<String, AttachmentPart> attachments = new HashMap<>();
    private MimeMultipart rootMultipart;
    private MimeMultipart relatedMultipart = new MimeMultipart(ContentType.RELATED.getValue());
    private Session session;

    public BaseMailMessage(Session session, String charset, ContentType rootContentType) {
        this.session = session;
        this.rootContentType = rootContentType;
        this.charset = charset;
        initialize();
    }

    private void initialize() {
        rootMimeMessage = new RootMimeMessage(session);
        rootMultipart = new MimeMultipart(rootContentType.getValue());
        setSentDate(Instant.now());

        try {

            rootMimeMessage.setContent(rootMultipart);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to set RootMultiPart", e);
        }

        initializeMessageId();

    }

    public void addRecipient(RecipientType recipientType, InternetAddress emailAddress) {
        try {
            rootMimeMessage.addRecipient(recipientType.asJavaMailType(), emailAddress);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add recipient " + recipientType + ": " + emailAddress.toString() + " to MIME message", e);
        }
    }

    public void addRecipients(RecipientType recipientType, InternetAddress[] emailAddresses) {
        try {
            rootMimeMessage.addRecipients(recipientType.asJavaMailType(), emailAddresses);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add " + recipientType + ":  Collection<Recipients>to MIME message", e);
        }
    }

    public void addRecipients(RecipientType recipientType, Collection<InternetAddress> emailAddresses) {
        try {
            rootMimeMessage.addRecipients(recipientType.asJavaMailType(), MailUtility.getInternetAddressses(emailAddresses));
        }
        catch (MessagingException e) {
            //Noop
        }
    }

    public void setFrom(InternetAddress emailAddress) {
        try {
            rootMimeMessage.setFrom(emailAddress);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to From Addresses", e);
        }
    }

    public BaseMailMessage setFrom(Collection<InternetAddress> emailAddresses) {
        try {
            if (emailAddresses.size() > 0) {
                rootMimeMessage.addFrom(MailUtility.getInternetAddressses(emailAddresses));
            }
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to From Addresses", e);
        }
        return this;
    }

    public void setReplyTo(String address) throws InvalidAddressException {
        setReplyTo(MailUtility.internetAddress(address));
    }

    public void setReplyTo(String name, String address) {
        setReplyTo(MailUtility.internetAddress(address, name));
    }

    public void setReplyTo(InternetAddress emailAddress) {
        List<InternetAddress> emailAddresses = new ArrayList<>();
        emailAddresses.add(emailAddress);
        setReplyTo(emailAddresses);
    }

    public void setReplyTo(Collection<InternetAddress> emailAddresses) {
        try {
            rootMimeMessage.setReplyTo(MailUtility.getInternetAddressses(emailAddresses));
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to set Reply-To", e);
        }
    }

    public void setSubject(String value) {
        setSubject(value, charset);
    }

    private void setSubject(String value, String charset) {
        try {
            rootMimeMessage.setSubject(value, charset);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add subject:" + value + " to MIME message with charset: " + charset, e);
        }
    }

    public void setSentDate(Instant instant) {
        try {
            rootMimeMessage.setSentDate(Date.from(instant));
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to set Sent Date on MimeMessage", e);
        }
    }

    public void setMessageID(String messageId) {
        rootMimeMessage.setMessageId("<" + messageId + ">");
    }

    private void initializeMessageId() {
        String mailerDomainName = session.getProperty(MailUtility.DOMAIN_PROPERTY_KEY);

        if (mailerDomainName != null && mailerDomainName.length() > 0) {
            setMessageID(UUID.randomUUID() + "@" + mailerDomainName);
        }
        else {
            setMessageID(UUID.randomUUID() + "@" + MailUtility.getHostName());
        }
    }

    public void setEnvelopeFrom(String value) {
        rootMimeMessage.setEnvelopeFrom(value);
    }

    public void addDeliveryRecieptAddresses(Collection<InternetAddress> addresses) {
        for (InternetAddress address : addresses) {
            addDeliveryReciept(address.getAddress());
        }
    }

    public void addReadRecieptAddresses(Collection<InternetAddress> addresses) {
        for (InternetAddress address : addresses) {
            addReadReciept(address.getAddress());
        }
    }

    public void addDeliveryReciept(String address) {
        addHeader(new Header(MailHeader.DELIVERY_RECIEPT.headerValue(), "<" + address + ">"));
    }

    public void addReadReciept(String address) {
        addHeader(new Header(MailHeader.READ_RECIEPT.headerValue(), "<" + address + ">"));
    }

    public void setImportance(MessagePriority messagePriority) {
        if (messagePriority != null && messagePriority != MessagePriority.NORMAL) {
            setHeader(new Header("X-Priority", messagePriority.getX_priority()));
            setHeader(new Header("Priority", messagePriority.getPriority()));
            setHeader(new Header("Importance", messagePriority.getImportance()));
        }
    }

    public void setHeader(Header header) {
        try {
            rootMimeMessage.setHeader(header.getName(), header.getValue());
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to SET Header: + " + header.getName() + " to Value: " + header.getValue(), e);
        }
    }

    public void addHeaders(Collection<Header> headers) {
        for (Header header : headers) {
            addHeader(header);
        }
    }

    public void addHeader(Header header) {
        try {
            rootMimeMessage.addHeader(header.getName(), header.getValue());
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to ADD Header: + " + header.getName() + " to Value: " + header.getValue(), e);
        }
    }

    public void setText(String text) {
        try {
            rootMultipart.addBodyPart(buildTextBodyPart(text));
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add TextBody to MimeMessage", e);
        }
    }

    public void setHTML(String html) {
        MimeBodyPart relatedBodyPart = new MimeBodyPart();
        try {
            relatedMultipart.addBodyPart(buildHTMLBodyPart(html));
            relatedBodyPart.setContent(relatedMultipart);
            rootMultipart.addBodyPart(relatedBodyPart);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add TextBody to MimeMessage", e);
        }
    }

    public void setHTMLNotRelated(String html) {
        try {
            rootMultipart.addBodyPart(buildHTMLBodyPart(html));
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add TextBody to MimeMessage", e);
        }
    }

    public void setHTMLTextAlt(String html, String text) {
        MimeBodyPart mixedBodyPart = new MimeBodyPart();

        MimeBodyPart relatedBodyPart = new MimeBodyPart();

        MimeMultipart alternativeMultiPart = new MimeMultipart(ContentType.ALTERNATIVE.getValue());

        try {
            // Text must be the first or some HTML capable clients will fail to
            // render HTML bodyPart.
            alternativeMultiPart.addBodyPart(buildTextBodyPart(text));
            alternativeMultiPart.addBodyPart(buildHTMLBodyPart(html));

            relatedBodyPart.setContent(alternativeMultiPart);

            relatedMultipart.addBodyPart(relatedBodyPart);

            mixedBodyPart.setContent(relatedMultipart);

            rootMultipart.addBodyPart(mixedBodyPart);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to build HTML+Text Email", e);
        }
    }

    public void setCalendar(String body, AttachmentPart invite) {
        try {
            rootMultipart.addBodyPart(buildHTMLBodyPart(body));
            rootMultipart.addBodyPart(invite);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to add Calendar Body to MimeMessage", e);
        }
    }

    private MimeBodyPart buildTextBodyPart(String text) {
        MimeBodyPart textBodyPart = new MimeBodyPart();

        try {
            textBodyPart.setDisposition(ContentDisposition.INLINE.headerValue());
            textBodyPart.setText(text, charset);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to build TextBodyPart", e);
        }

        return textBodyPart;
    }

    private MimeBodyPart buildHTMLBodyPart(String html) {
        MimeBodyPart htmlBodyPart = new MimeBodyPart();

        try {
            htmlBodyPart.setDisposition(ContentDisposition.INLINE.headerValue());
            htmlBodyPart.setText(html, charset, "html");
        }
        catch (MessagingException e) {
            throw new RuntimeException("Unable to build HTMLBodyPart", e);
        }

        return htmlBodyPart;
    }

    public void addAttachment(BaseAttachment emailAttachment) {
        String contentId = emailAttachment.getContentDisposition() == ContentDisposition.INLINE ? emailAttachment.getContentId() : null;
        AttachmentPart attachment = new AttachmentPart(emailAttachment.getBytes(), contentId, emailAttachment.getFileName(), emailAttachment.getMimeType(), emailAttachment.getHeaders(),
                emailAttachment.getContentDisposition());
        attachments.put(attachment.getAttachmentFileName(), attachment);
    }

    public void addAttachments(Collection<BaseAttachment> emailAttachments) {
        for (BaseAttachment ea : emailAttachments) {
            addAttachment(ea);
        }
    }

    public Map<String, AttachmentPart> getAttachments() {
        return attachments;
    }

    public SMTPMessage getRootMimeMessage() {
        return rootMimeMessage;
    }

    public SMTPMessage getFinalizedMessage() {
        addAttachmentsToMessage();
        return getRootMimeMessage();
    }

    private void addAttachmentsToMessage() {
        for (AttachmentPart a : attachments.values()) {
            if (a.getContentDisposition() == ContentDisposition.ATTACHMENT) {
                try {
                    rootMultipart.addBodyPart(a);
                }
                catch (MessagingException e) {
                    throw new RuntimeException("Unable to Add STANDARD Attachment: " + a.getAttachmentFileName(), e);
                }
            }
            else if (a.getContentDisposition() == ContentDisposition.INLINE) {
                try {
                    if (relatedMultipart.getCount() > 0) {
                        relatedMultipart.addBodyPart(a);
                    }
                    else {
                        rootMultipart.addBodyPart(a);
                    }
                }
                catch (MessagingException e) {
                    throw new RuntimeException("Unable to Add INLINE Attachment: " + a.getAttachmentFileName(), e);
                }
            }
            else {
                throw new RuntimeException("Unsupported Attachment Content Disposition");
            }
        }
    }
}
