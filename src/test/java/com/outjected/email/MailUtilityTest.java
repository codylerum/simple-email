package com.outjected.email;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import com.google.common.io.Resources;
import com.outjected.email.api.ContentDisposition;
import com.outjected.email.api.EmailMessage;
import com.outjected.email.impl.util.MailUtility;
import com.outjected.email.impl.util.MessageConverter;
import org.junit.Assert;
import org.junit.Test;

public class MailUtilityTest {

    @Test
    public void determineCharset() throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.addHeader("Content-Type", "text/plain;charset=utf-8; Content-Transfer-Encoding:base64");
        Assert.assertEquals(StandardCharsets.UTF_8, MailUtility.determineCharset(part).orElseThrow(RuntimeException::new));
    }

    @Test
    public void determineCharsetNoSemiColon() throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.addHeader("Content-Type", "text/plain;charset=utf-8");
        Assert.assertEquals(StandardCharsets.UTF_8, MailUtility.determineCharset(part).orElseThrow(RuntimeException::new));
    }

    @Test
    public void decodeString() {
        Assert.assertNull(MailUtility.decodeString(null));
        Assert.assertEquals("Invoice.pdf", MailUtility.decodeString("=?utf-8?B?SW52b2ljZS5wZGY=?="));
        Assert.assertEquals("Invoice.pdf", MailUtility.decodeString("=?utf-8?b?SW52b2ljZS5wZGY=?="));
        Assert.assertEquals("Invoice.pdf", MailUtility.decodeString("Invoice.pdf"));
        Assert.assertEquals("Invoice.pdf", MailUtility.decodeString("=?us-ascii?Q?Invoice.pdf?="));
        Assert.assertEquals("Invoice.pdf", MailUtility.decodeString("=?utf-8?q?Invoice.pdf?="));
        Assert.assertEquals("this is some text", MailUtility.decodeString("=?iso-8859-1?q?this=20is=20some=20text?="));
    }

    @Test
    public void converterMultipartWithAttachment() throws IOException, MessagingException {
        try (InputStream inputStream = Resources.getResource("html-and-text-mime").openStream()) {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
            EmailMessage emailMessage = MessageConverter.convert(mimeMessage);
            Assert.assertEquals(1, emailMessage.getAttachments().size());
            Assert.assertEquals("test.csv", emailMessage.getAttachments().get(0).getFileName());
            Assert.assertTrue(emailMessage.getTextBody().startsWith("Ticket T29345"));
            Assert.assertTrue(emailMessage.getHtmlBody().startsWith("<!DOCTYPE html PUBLIC"));
            Assert.assertEquals("Ticket T29345 - New Comment", emailMessage.getSubject());
        }
    }

    @Test
    public void converterAttachmentNoBody() throws IOException, MessagingException {
        try (InputStream inputStream = Resources.getResource("attachment-no-body.mime").openStream()) {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
            EmailMessage emailMessage = MessageConverter.convert(mimeMessage);
            Assert.assertEquals(1, emailMessage.getAttachments().size());
            Assert.assertEquals("Messages.pdf", emailMessage.getAttachments().get(0).getFileName());
            Assert.assertEquals(ContentDisposition.ATTACHMENT, emailMessage.getAttachments().get(0).getContentDisposition());
            Assert.assertNull(emailMessage.getTextBody());
            Assert.assertNull(emailMessage.getHtmlBody());
            Assert.assertEquals("Test Subject", emailMessage.getSubject());
        }
    }

    @Test
    public void parseQpInputStream() throws IOException, MessagingException {
        try (InputStream inputStream = Resources.getResource("qp-inputstrem.mime").openStream()) {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
            EmailMessage emailMessage = MessageConverter.convert(mimeMessage);
            Assert.assertEquals(1, emailMessage.getAttachments().size());
            Assert.assertEquals("05062021scan05062021.pdf", emailMessage.getAttachments().get(0).getFileName());
            Assert.assertEquals(ContentDisposition.ATTACHMENT, emailMessage.getAttachments().get(0).getContentDisposition());
            Assert.assertTrue(emailMessage.getTextBody().contains("HIPAA CONFIDENTIALITY NOTICE"));
            Assert.assertNull(emailMessage.getHtmlBody());
            Assert.assertEquals("SWC Scanned", emailMessage.getSubject());
        }
    }

    @Test
    public void textOnly() throws IOException, MessagingException {
        try (InputStream inputStream = Resources.getResource("text-only.mime").openStream()) {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
            EmailMessage emailMessage = MessageConverter.convert(mimeMessage);
            Assert.assertTrue(emailMessage.getTextBody().contains("Testing 1,2,3,4"));
            Assert.assertNull(emailMessage.getHtmlBody());
            Assert.assertEquals("Plaintext Test", emailMessage.getSubject());
        }
    }

    @Test
    public void htmlOnly() throws IOException, MessagingException {
        try (InputStream inputStream = Resources.getResource("html-only.mime").openStream()) {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
            EmailMessage emailMessage = MessageConverter.convert(mimeMessage);
            Assert.assertTrue(emailMessage.getHtmlBody().contains("Outlook for Android"));
            Assert.assertNull(emailMessage.getTextBody());
            Assert.assertEquals("Test fax", emailMessage.getSubject());
        }
    }
}
