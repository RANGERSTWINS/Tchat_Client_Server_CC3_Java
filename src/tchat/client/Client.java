package tchat.client;
import tchat.ITchat;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.io.*;
import java.util.Set;
import java.nio.charset.Charset;
import java.util.Iterator;


/**
 * Client de tchat
 */
public class Client extends Thread implements ITchat {

    private ClientUI clientUI;
    private String hostname;
    private int port;
    public String pseudo;
    private SocketChannel sc;
    private Selector selecteur;
    private Charset charset = Charset.forName("UTF-8");

    public Client(ClientUI clientUI, String hostname, int port, String pseudo) {
      this.clientUI = clientUI;
      this.hostname = hostname;
      this.port = port;
      this.pseudo = pseudo;
      try{
      selecteur = Selector.open();
      this.sc = SocketChannel.open(new InetSocketAddress(hostname, port));
      sc.configureBlocking(false);
      sc.register(selecteur, SelectionKey.OP_READ);
    }
      catch(IOException e){
        System.out.println("Exception throw dans le constructeur de la classe client");
        e.getStackTrace();
      }
    }

    /**
     * Ajoute un message et passe le channel en
     * mode ecriture
     */
    public void addMessage(String message) {
      try {
        byte[] res = new String(message).getBytes();
        ByteBuffer buf = ByteBuffer.wrap(res);
        buf.clear();
        sc.register(selecteur, SelectionKey.OP_WRITE);
        buf.put(message.getBytes());
        buf.flip();
        sc.write(buf);
        
      } catch(Exception e){
        System.out.println("Exception throw dans la méthode addMessage de la classe client");
        e.getStackTrace();
      }
    }

    /**
     * Process principal du thread
     * on ecoute
     */
    public void run() {
      try
      {
          while(clientUI.isRunning()) {
              int readyChannels = selecteur.select();
              if(readyChannels == 0) continue; 
              Set selectedKeys = selecteur.selectedKeys();
              Iterator keyIterator = selectedKeys.iterator();
              while(keyIterator.hasNext()) {
                   SelectionKey sk = (SelectionKey) keyIterator.next();
                   sk.interestOps(SelectionKey.OP_READ);
                   keyIterator.remove();
                   readSelectKey(sk);
              }
          }
      }
      catch (IOException io)
      {}
    }

    private void readSelectKey(SelectionKey sk) throws IOException {
      if(sk.isReadable()){
          SocketChannel sc = (SocketChannel)sk.channel();

          ByteBuffer buf = ByteBuffer.allocate(1024);
          String message = "";
          while(sc.read(buf) > 0)
          {
            buf.flip();
            message = message + charset.decode(buf);
          }
          clientUI.appendMessage("" + message + "\n");
      }
  }
}


