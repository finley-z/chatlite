package chatlite.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * @author zyf
 * @date 2017/9/22
 */
public class ServerHandler {
    private Selector selector;
    private int serverPort;
    private ServerSocketChannel serverSocketChannel;
    private SimpleDateFormat dateFormat;

    public ServerHandler() {
        this(8080);
    }

    /***
     * 初始化服务器
     * @param serverPort
     */
    public ServerHandler(int serverPort) {
        this.serverPort = serverPort;

        try{
            serverSocketChannel = ServerSocketChannel.open();
            selector = Selector.open();
            dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SS");
        }catch (Exception e){
            System.out.println("当前时间:"+dateFormat.format(new Date())+";服务器初始化失败!");
            e.printStackTrace();
        }
    }

    public void startServer() {
        try {
            System.out.println("当前时间:"+dateFormat.format(new Date())+";服务器正在启动!");
            serverSocketChannel.bind(new InetSocketAddress(serverPort));
            serverSocketChannel.configureBlocking(false);
            //监听客户端的连接事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                if (selector.selectNow()==0) {
                    continue;
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isConnectable()) {
                        handleConnect(key);
                    }
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if (key.isWritable()) {
                        handleWrite(key);
                    }
                    it.remove();
                }
            }

        } catch (IOException e) {
            System.out.println("server init fail......");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void handleConnect(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            System.out.println(channel.getRemoteAddress() + " 上线了");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleAccept(SelectionKey key) throws IOException, InterruptedException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        //给客户端反馈，连接成功
        System.out.println("客户端:"+client.getRemoteAddress()+"上线了");
        client.write(ByteBuffer.wrap(new String(dateFormat.format(new Date())+"  连接成功!").getBytes()));
        // 在客户端连接成功之后，为了可以接收到客户端的信息，需要给通道设置读的权限
        client.register(this.selector, SelectionKey.OP_READ);
//        client.register(this.selector, SelectionKey.OP_WRITE);
    }

    public void handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
//      CharBuffer charBuffer = CharBuffer.allocate(1024);
//      CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
        long length = 0;
        StringBuilder stringBuilder=new StringBuilder();
        try {
            while ((length = channel.read(buffer)) > 0) {
                buffer.flip();
                if (buffer.hasRemaining()) {
                  stringBuilder.append(new String(buffer.array()));
                }
            }
            if (length == -1) {
                channel.close();
            }
            buffer.clear();
            System.out.println("客户端:"+channel.getRemoteAddress()+"说 "+stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleWrite(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        CharsetEncoder decoder = Charset.defaultCharset().newEncoder();
        buffer.flip();
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.compact();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[]args){
        new ServerHandler().startServer();
    }
}
