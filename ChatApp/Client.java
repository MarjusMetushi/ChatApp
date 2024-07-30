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
import java.net.Socket;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Client extends JFrame{
    private static JPanel panel;
    private static JPanel messagePanel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    //Let's construct the GUI 
    public Client(String address, int port){
        //Setting the frame title and size
        super("Client Side");
        setSize(800, 800);
        //Constructing the window where the chats will be shown
        JLabel chatText = new JLabel("Initiate a conversation");
        chatText.setFont(new Font("Arial", Font.BOLD, 20));
        chatText.setForeground(Color.BLACK);
        //Taking a background for the chat app
        Image background = Toolkit.getDefaultToolkit().createImage("C:\\Users\\mariu\\Desktop\\JavaChat\\chatbackground.jpg");
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
                float opacity = 0.3f;
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
        try {
            //Establishing connection with the server
            socket = new Socket("localhost",12345);
            //Variables for input and output
            out = new PrintWriter(socket.getOutputStream(),true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //Starting a thread to receive input
            new Thread(new IncomingReader()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        
    }
    public static void main(String[] args) throws IOException {
        new Client("localhost",12345);
        System.out.println("Connected");
    }
    //Method to add messages to the panel
    private void addToPanel(String message){
        SwingUtilities.invokeLater(()->{
            JLabel label = new JLabel(message);
            label.setFont(new Font("Arial",Font.BOLD,20));
            messagePanel.add(label);
            messagePanel.revalidate();
            messagePanel.repaint();
        });
        
    }
    //Method that send the server the message
    private void send(String message) {
        out.println(message);
    }
    //Class used to take the input and display it on the GUI
    private class IncomingReader implements Runnable{
        @Override
        public void run() {
            String msg;
            try {
                //While the message is not empty we add it to the GUI
                while ((msg = in.readLine())!=null) {
                    addToPanel(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally{
                try {
                    //Safely closing the socket to avoid errors
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
