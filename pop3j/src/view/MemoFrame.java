package view;

import model.UserInfo;
import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class MemoFrame extends JFrame implements ActionListener {
    private final JPanel infoPanel = new JPanel();
    private final JPanel functionalPanel = new JPanel();
    private final JTextArea infoArea = new JTextArea();
    private final JScrollPane scrollPane = new JScrollPane(infoArea);
    private final JTextField commandField = new JTextField(15);
    private final JButton performBtn = new JButton("Perform");
    private final Properties props = new Properties();
    private UserInfo userInfo;
    private Boolean isConnected = false;
    private Message[] messages;
    private Store store;
    private Folder inbox;

    public MemoFrame() {
        super("Pop3 Client. Memo frame");
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();

        userInfo = new UserInfo("pop.gmail.com", "", "", "pop3");

        infoArea.setEditable(false);
        performBtn.addActionListener(this);

        functionalPanel.setLayout(new FlowLayout());
        functionalPanel.add(commandField);
        functionalPanel.add(performBtn);
        infoPanel.setLayout(new BorderLayout());
        infoPanel.add(scrollPane, BorderLayout.CENTER);
        infoPanel.add(functionalPanel, BorderLayout.SOUTH);

        getSwag();
        setLocation(dimension.width / 8 * 3, dimension.height / 5);
        setSize(dimension.width / 4, dimension.height / 2);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        add(infoPanel);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private void getConnection() {
        props.put("mail.host", "pop.gmail.com");
        props.put("mail.store.protocol", "pop3s");
        props.put("mail.pop3s.auth", "true");
        props.put("mail.pop3s.port", "995");

        try {
            Session session = Session.getInstance(props);
            store = session.getStore();
            store.connect(userInfo.getLogin(), userInfo.getPassword());
            inbox = store.getFolder("INBOX");

            if (inbox == null) {
                System.out.println("No INBOX");
                System.exit(1);
            }

            inbox.open(Folder.READ_WRITE);
            messages = inbox.getMessages();
            isConnected = true;

            infoArea.append("+OK " + userInfo.getLogin() + "'s maildrop has " + messages.length
                    + " messages\n");


        } catch (Exception ex) {
            ex.printStackTrace();
            infoArea.append("-ERR cannot get inbox messages\n");
        }
    }

    private void getSwag() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Nimbus is not available");
        }
    }

    private void getAuthorisation(String login) {
        infoArea.append("USER\n");
        userInfo.setLogin(login);
        if (crunchyEmailValidator(login)) {
            infoArea.append("+OK " + login + " is a real hoopy frood\n");
        } else {
            infoArea.append("-ERR check email address\n");
        }
    }

    private void getPassword(String password) {
        infoArea.append("PASS\n");
        userInfo.setPassword(password);
        getConnection();
    }

    public void getList() {
        infoArea.append("LIST\n");
        if (isConnected) {
            infoArea.append("+OK " + userInfo.getLogin() + " has " + messages.length + " messages\n");
        } else {
            infoArea.append("-ERR not connected\n");
        }
    }

    public void closeConnection() {
        infoArea.append("QUIT\n");
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(true);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
            isConnected = false;
            infoArea.append("+OK connection closed\n");
        } catch (MessagingException e) {
            infoArea.append("-ERR something went wrong\n");
            e.printStackTrace();
        }
    }

    public void getStatistic() {
        infoArea.append("STAT\n");
        if (isConnected) {
            infoArea.append("+OK " + messages.length + " (" + getInboxSize() + " octets)\n");
        } else {
            infoArea.append("-ERR not connected\n");
        }
    }

    public void deleteMsg(int number) {
        infoArea.append("DELE\n");
        if (isConnected) {
            try {
                messages[number - 1].setFlag(Flags.Flag.DELETED, true); // Corrected to 1-based index
                infoArea.append("+OK message " + number + " is marked as DELETE\n");
            } catch (MessagingException e) {
                infoArea.append("-ERR cannot delete message\n");
                e.printStackTrace();
            }
        } else {
            infoArea.append("-ERR not connected\n");
        }
    }

    private String getAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (Address address : addresses) {
            sb.append(((InternetAddress) address).toUnicodeString()).append(", ");
        }
        return sb.substring(0, sb.length() - 2); // Remove the trailing comma and space
    }

    public void retrieveMsg(int number) {
        infoArea.append("RETR\n");
        if (isConnected) {
            try {
                Message message = messages[number - 1]; // Corrected to 1-based index
                String textFromMessage = getTextFromMessage(message);
                infoArea.append("Message " + number + "\n");
                infoArea.append("From: " + getAddresses(message.getFrom()) + "\n");
                infoArea.append("Subject: " + message.getSubject() + "\n");
                infoArea.append("Sent Date: " + message.getSentDate() + "\n");
                infoArea.append("Text: " + textFromMessage + "\n");
                infoArea.append("--- END OF MESSAGE ---\n");
            } catch (Exception ex) {
                infoArea.append("-ERR something wrong with retrieving messages\n");
                ex.printStackTrace();
            }
        } else {
            infoArea.append("-ERR not connected\n");
        }
    }

    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            StringBuilder result = new StringBuilder();
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result.append(bodyPart.getContent());
                    break;
                } else if (bodyPart.isMimeType("text/html")) {
                    String html = (String) bodyPart.getContent();
                    result.append(Jsoup.parse(html).text());
                }
            }
            return result.toString();
        }
        return "";
    }

    public void testConnection() {
        infoArea.append("NOOP\n");
        if (store != null && store.isConnected()) {
            infoArea.append("+OK connection is active\n");
        } else {
            infoArea.append("-ERR connection lost\n");
        }
    }

    public void showHelp() {
        infoArea.append("HELP\n");
        infoArea.append("USER, PASS, LIST, STAT, DELE, RETR, NOOP, QUIT\n");
    }

    public int getInboxSize() {
        int inboxSize = 0;
        for (Message message : messages) {
            try {
                inboxSize += message.getSize();
            } catch (MessagingException e) {
                infoArea.append("-ERR inbox size calculation problem\n");
                e.printStackTrace();
            }
        }
        return inboxSize;
    }

    private boolean crunchyEmailValidator(String email) {
        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate();
            return true;
        } catch (AddressException e) {
            System.out.println("Exception Occurred for: " + email);
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == performBtn) {
            String commandPart = commandField.getText().trim();
            String variablePart = null;
            int numberOfMessage = 1;

            String[] parts = commandPart.split("\\s+", 2);
            commandPart = parts[0];

            if (parts.length > 1) {
                variablePart = parts[1];
                try {
                    numberOfMessage = Integer.parseInt(variablePart);
                } catch (NumberFormatException ignored) {
                }
            }

            switch (commandPart.toUpperCase()) {
                case "USER":
                    if (variablePart != null && !variablePart.isEmpty()) {
                        getAuthorisation(variablePart);
                    } else {
                        infoArea.append("-ERR USER command requires an argument\n");
                    }
                    break;
                case "PASS":
                    if (variablePart != null && !variablePart.isEmpty()) {
                        getPassword(variablePart);
                    } else {
                        infoArea.append("-ERR PASS command requires an argument\n");
                    }
                    break;
                case "LIST":
                    getList();
                    break;
                case "STAT":
                    getStatistic();
                    break;
                case "DELE":
                    if (variablePart != null && !variablePart.isEmpty()) {
                        deleteMsg(numberOfMessage);
                    } else {
                        infoArea.append("-ERR DELE command requires a message number\n");
                    }
                    break;
                case "RETR":
                    if (variablePart != null && !variablePart.isEmpty()) {
                        retrieveMsg(numberOfMessage);
                    } else {
                        infoArea.append("-ERR RETR command requires a message number\n");
                    }
                    break;
                case "NOOP":
                    testConnection();
                    break;
                case "HELP":
                    showHelp();
                    break;
                case "QUIT":
                    closeConnection();
                    break;
                default:
                    infoArea.append("-ERR \"" + commandPart + "\" no such command\n");
            }
        }
    }
}
