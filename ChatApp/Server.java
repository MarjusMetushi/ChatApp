import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Server extends JFrame {
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static JPanel panel;
    private static JPanel messagePanel;
    
    //Let's construct the GUI
    public Server(){
        //Setting the frame title and size
        super("Server Side");
        setSize(800, 800);
        //Constructing the window where the chats will be shown
        JLabel chatText = new JLabel("Initiate a conversation");
        chatText.setFont(new Font("Arial", Font.BOLD, 20));
        chatText.setForeground(Color.BLACK);
        //Taking a background for the chat app
        Image background = Toolkit.getDefaultToolkit().createImage("chatbackground.jpg");
        //Making a panel to add the chatText label
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(700, 700));
        panel.add(chatText, BorderLayout.NORTH);
        //Building the message panel in which the background is implemented at 20% opacity
        messagePanel = new JPanel(){
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                float opacity = 0.2f;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2d.drawImage(background, 0, 0, getWidth(), getHeight(), this);
                g2d.dispose();
            }
        };
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        //Making a scrollPane for the messages
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        panel.add(scrollPane);
        //Making a mini panel to insert the send button and text area for aesthetic purposes
        JPanel miniPanel = new JPanel();
        miniPanel.setLayout(new BorderLayout());
        //Making a send button to send the messages
        JButton send = new JButton("Send");
        send.setFont(new Font("Arial",Font.BOLD,12));
        send.setBackground(new Color(255,255,0));
        send.setFocusable(false);
        //Making a text area to be able to write the message inside of it
        JTextArea text = new JTextArea(2, 10);
        text.setFont(new Font("Verdana",Font.BOLD,18));
        //Making a scrollPane for the text
        JScrollPane textScrollPane = new JScrollPane(text);
        //Adding everything together and to the main panel
        miniPanel.add(textScrollPane, BorderLayout.CENTER);
        miniPanel.add(send, BorderLayout.EAST);
        panel.add(miniPanel, BorderLayout.SOUTH);
        //Setting the layout to center everything in the frame
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(panel, gbc);

        //Adding function to the send button
        send.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                send(text.getText());
                text.setText("");
            }
            
        });
        //Using the enter button to also send messages
        text.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e){
                //Handling cases where shift and enter are pressed together 
                if (e.getKeyCode()==KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    //Preventing newline character
                    e.consume(); 
                    send(text.getText());
                    text.setText("");
                }
            }
        });
        
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }
    //Let's start the server on port12345, accept the connection and start the frame
    public static void main(String[] args) throws IOException {
        System.out.println("Chat server started");
        new Server();
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        }
        
    }
   //Method to send a message to Clients by using our messages inside of our Set
    private void send(String message) {
       synchronized(clientWriters){
        //Displaying the message for the server to view it
        for (PrintWriter printWriter : clientWriters) {
            printWriter.println("Server: "+message);
            }
        }
        //Making our message visual in the GUI
        addToScreen("Server: "+message);
    }
    
    //Method to add Server's message to the GUI
    private void addToScreen(String message) {
        SwingUtilities.invokeLater(()->{
            //Adding our message to a label that will be added in our message Panel
            JLabel label = new JLabel(message);
            label.setFont(new Font("Arial",Font.BOLD,20));
            messagePanel.add(label); 
            messagePanel.revalidate();
            messagePanel.repaint();
        });
    }
    //Making a client handler class to be able to handle the other client to connect to the server
    private static class ClientHandler extends Thread{

        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket){
            this.socket = socket;
        }
        public void run(){
            try {
                //Taking the input from the socket's input stream
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //Taking the output from socket's output stream
                out = new PrintWriter(socket.getOutputStream(),true);
                //Adding our output to the Set
                synchronized(clientWriters){
                    clientWriters.add(out);
                }
                String msg;
                //broadcasting each message of the input
                while ((msg = in.readLine()) != null) {
                    broadcast("Client "+": "+msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //Safely closing the socket to avoid errors
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //We remove the broadcasted words from our set
                synchronized(clientWriters){
                    clientWriters.remove(out);
                }
            }
        }
        //Sending the message to all the connected clients and updating the GUI
        private void broadcast(String message) {
            synchronized(clientWriters){
                for (PrintWriter printWriter : clientWriters) {
                    printWriter.println(message);
                }
            }
            //Adding the message to the clients GUI
            SwingUtilities.invokeLater(()->{
                JLabel label = new JLabel(message);
                label.setFont(new Font("Arial",Font.BOLD,20));
                messagePanel.add(label); 
                messagePanel.revalidate();
                messagePanel.repaint();
            });
        }
    }
}