package tchat.server;
import tchat.ITchat;


import javafx.application.Platform;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.util.HashSet;

/**
 * Processus serveur qui ecoute les connexion entrantes,
 * les messages entrant et les rediffuse au clients connectes
 *
 * @author mathieu.fabre
 */
public class Server extends Thread implements ITchat {

    /**
     * Interface graphique du serveur
     */
    private ServerUI serverUI;
    private Charset charset = Charset.forName("UTF-8");
    private String ip;
    private int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private static String split = "__";

    /**
     * Constructeur
     * Lien avec l interface Graphique
     * Creation du selecteur et du socket serveur
     *
     * @param serverUI
     */
    public Server(ServerUI serverUI, String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.serverUI = serverUI;

        try{
        this.selector = Selector.open();
        this.ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        InetSocketAddress inetSocket = new InetSocketAddress(ip, port);
        ssc.bind(inetSocket);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        sendLogToUI("Serveur créer");
        } 
        catch(IOException e){
            System.out.println("Exception throw dans le constructeur de la classe Server");
            e.printStackTrace();
        }
    }

    /**
     * Envoi un message de log a l'IHM
     */
    public void sendLogToUI(String message) {
        Platform.runLater(() -> serverUI.log(message));
    }

    /**
     * Process principal du server
     */
    public void run(){
        try{
        while(serverUI.isRunning()) {
            int readyChannels = selector.select();
            if(readyChannels == 0) continue; 
            Set selectedKeys = selector.selectedKeys();
            Iterator keyIterator = selectedKeys.iterator();
            while(keyIterator.hasNext()) {
                 SelectionKey sk = (SelectionKey) keyIterator.next();
                 keyIterator.remove();
                 selectionKeyType(ssc,sk);
            }
        }
        } catch(Exception e){
            System.out.println("Exception throw dans la méthode run de la classe Server");
            e.printStackTrace();
        }
    } 
		
    public void selectionKeyType(ServerSocketChannel scc,SelectionKey sk) throws IOException {
        if(sk.isAcceptable()){
            //On accepte la connection
            SocketChannel sc = scc.accept();
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_READ);
            
            sk.interestOps(SelectionKey.OP_ACCEPT);
            sendLogToUI("Connection du client sur :" + sc.getRemoteAddress());
        }
        if(sk.isReadable()){
            SocketChannel sc = (SocketChannel)sk.channel(); 
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuilder message = new StringBuilder();
            try{
                while(sc.read(buffer) > 0){
                    buffer.flip();
                    message.append(charset.decode(buffer));
                }
                sk.interestOps(SelectionKey.OP_READ);
            }
            catch (IOException io){
                sk.cancel();
                sk.channel().close();
            }
            if(message.length() > 0){
                String[] arrayContent = message.toString().split(split);;
                String messageFinal = arrayContent[0];
                BroadCast(messageFinal, selector);
            } 
        }
    }

//Envoie du message à tout les clients connecté
    public void BroadCast(String message, Selector selector) throws IOException {
        //Pour chaque clé utilisé du selecteur, soit chaque client connecté au serveur
        for(SelectionKey key : selector.keys())
        {
            Channel targetchannel = key.channel();
            if(targetchannel instanceof SocketChannel && targetchannel!=null)
            {
                SocketChannel dest = (SocketChannel)targetchannel;
                dest.write(charset.encode(message));
            }
        }
    }
}
