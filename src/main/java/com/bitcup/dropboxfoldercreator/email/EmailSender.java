package com.bitcup.dropboxfoldercreator.email;

import com.bitcup.dropboxfoldercreator.config.Config;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @author bitcup
 */
public class EmailSender {

    private final Properties mailServerProperties;

    public static void main(String[] args) throws Exception {
        EmailSender emailSender = EmailSender.getInstance();
        String body = "Test email." + "<br><br> Regards, <br>Admin";
        emailSender.sendMail(Config.val("test.student1.email"), "Testing", body);
    }

    private static final EmailSender instance = new EmailSender();

    private EmailSender() {
        mailServerProperties = System.getProperties();
        mailServerProperties.put("mail.smtp.port", "587");
        mailServerProperties.put("mail.smtp.auth", "true");
        mailServerProperties.put("mail.smtp.starttls.enable", "true");
    }

    public static EmailSender getInstance() {
        return instance;
    }

    public void sendMail(String recipient, String subject, String emailBody) throws MessagingException {
        final Session session = Session.getDefaultInstance(mailServerProperties, null);
        final MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        mimeMessage.setSubject(subject);
        mimeMessage.setContent(emailBody, "text/html");

        Transport transport = session.getTransport("smtp");
        transport.connect("smtp.gmail.com", Config.val("gmail.user"), Config.val("gmail.password"));
        transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
        transport.close();
    }
}
