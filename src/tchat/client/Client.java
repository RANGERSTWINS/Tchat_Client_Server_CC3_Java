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
    public String nickname;
    private SocketChannel sc;
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");

    public Client(ClientUI clientUI, String hostname, int port, String nickname) {
      this.clientUI = clientUI;
      this.hostname = hostname;
      this.port = port;
      this.nickname = nickname;
      try{
      selector = Selector.open();
      this.sc = SocketChannel.open(new InetSocketAddress(hostname, port));
      sc.configureBlocking(false);
      sc.register(selector, SelectionKey.OP_READ);
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
        byte[] result = new String(message).getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.clear();
        sc.register(selector, SelectionKey.OP_WRITE);
        buffer.put(message.getBytes());
        buffer.flip();
        sc.write(buffer);
        
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
              int readyChannels = selector.select();
              if(readyChannels == 0) continue; 
              Set selectedKeys = selector.selectedKeys();
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

          ByteBuffer buffer = ByteBuffer.allocate(1024);
          String message = "";
          while(sc.read(buffer) > 0)
          {
            buffer.flip();
            message = message + charset.decode(buffer);
          }
          clientUI.appendMessage("" + message + "\n");
      }
  }
}


